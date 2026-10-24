package com.example.employeemanager.salary;

import com.example.employeemanager.data.AppDB;
import com.example.employeemanager.data.dto.SalaryBreakdown;
import com.example.employeemanager.data.entities.Employee;
import com.example.employeemanager.data.entities.Salary;
import com.example.employeemanager.data.entities.WorkLog;
import com.example.employeemanager.util.Constants;

import java.time.Clock;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * Lớp dịch vụ quản lý bảng lương của nhân viên.
 *
 * <p>Đảm bảo toàn vẹn nghiệp vụ qua các giao dịch Room (transaction), kiểm tra phân quyền,
 * ngăn ngừa trùng lặp và duy trì tính bất biến của các bảng lương đã thanh toán (PAID).</p>
 */
public class SalaryService {

    private final AppDB appDb;
    private final SalaryAuthorization authorization;
    private final SalaryCalculator calculator;
    private final Clock clock;
    private final ZoneId zoneId;

    /**
     * Khởi tạo SalaryService với đầy đủ các thành phần phụ thuộc (dùng trong sản phẩm hoặc kiểm thử).
     */
    public SalaryService(AppDB appDb,
                         SalaryAuthorization authorization,
                         SalaryCalculator calculator,
                         Clock clock) {
        this.appDb = Objects.requireNonNull(appDb, "Cơ sở dữ liệu AppDB không được để trống");
        this.authorization = Objects.requireNonNull(authorization, "Bộ phân quyền không được để trống");
        this.calculator = Objects.requireNonNull(calculator, "SalaryCalculator không được để trống");
        this.clock = Objects.requireNonNull(clock, "Clock không được để trống");
        this.zoneId = ZoneId.of(Constants.TIME_ZONE_ID);
    }

    /**
     * Khởi tạo SalaryService sử dụng đồng hồ mặc định theo múi giờ nghiệp vụ.
     */
    public SalaryService(AppDB appDb, SalaryAuthorization authorization) {
        this(appDb, authorization, new SalaryCalculator(), Clock.system(ZoneId.of(Constants.TIME_ZONE_ID)));
    }

    /**
     * Tạo mới hoặc tính lại bảng lương tháng cho một nhân viên.
     *
     * <p>Thao tác chỉ dành cho ADMIN. Nếu chưa có bảng lương, tạo mới với trạng thái DRAFT.
     * Nếu đã có bảng lương DRAFT, cập nhật lại số giờ và số tiền trên đúng bản ghi cũ.
     * Nếu bảng lương đã ở trạng thái PAID, từ chối tính lại.
     * Nếu trong tháng còn bất kỳ chấm công OPEN nào, từ chối thực hiện.</p>
     *
     * @param employeeId ID của nhân viên.
     * @param month      tháng tính lương (1–12).
     * @param year       năm tính lương.
     * @return đối tượng {@link Salary} đã được lưu hoặc cập nhật.
     */
    public Salary generateMonthlySalary(long employeeId, int month, int year) {
        authorization.requireAdmin();
        validateMonthYear(month, year);

        return runInTransaction(() -> {
            Employee employee = appDb.employeeDao().findById(employeeId);
            if (employee == null) {
                throw new IllegalArgumentException("Không tìm thấy nhân viên với ID: " + employeeId);
            }

            if (employee.hourlyRate <= 0) {
                throw new IllegalArgumentException("Đơn giá giờ của nhân viên phải lớn hơn 0");
            }

            String monthKey = formatMonthKey(year, month);

            Salary existingSalary = appDb.salaryDao().findByEmployeeAndMonth(employeeId, month, year);
            if (existingSalary != null && Constants.STATUS_PAID.equals(existingSalary.status)) {
                throw new IllegalStateException("Bảng lương đã thanh toán, không thể tính lại");
            }

            WorkLog openLog = appDb.workLogDao().findOpenByEmployeeAndMonth(employeeId, monthKey);
            if (openLog != null) {
                throw new IllegalStateException("Tháng này còn lượt chấm công chưa hoàn tất");
            }

            List<WorkLog> completedLogs = appDb.workLogDao().findCompletedByEmployeeAndMonth(employeeId, monthKey);
            SalaryBreakdown breakdown = calculator.calculateSalaryBreakdown(completedLogs, employee.hourlyRate);

            if (existingSalary == null) {
                Salary newSalary = new Salary(
                        employeeId,
                        month,
                        year,
                        breakdown.getTotalHours(),
                        breakdown.getOvertimeHours(),
                        breakdown.getTotalSalary(),
                        Constants.STATUS_DRAFT
                );
                long newId = appDb.salaryDao().insert(newSalary);
                newSalary.id = newId;
                return newSalary;
            } else {
                existingSalary.totalHours = breakdown.getTotalHours();
                existingSalary.overtimeHours = breakdown.getOvertimeHours();
                existingSalary.totalSalary = breakdown.getTotalSalary();
                existingSalary.status = Constants.STATUS_DRAFT;
                appDb.salaryDao().update(existingSalary);
                return existingSalary;
            }
        });
    }

    /**
     * Lấy bảng lương của một nhân viên theo tháng và năm.
     *
     * <p>Đây là hàm truy vấn nội bộ giữa các module (ví dụ ReportService gọi sau khi đã xác thực chủ sở hữu).
     * Không gọi trực tiếp từ giao diện nhân viên.</p>
     *
     * @param employeeId ID của nhân viên.
     * @param month      tháng (1–12).
     * @param year       năm.
     * @return đối tượng {@link Salary} hoặc {@code null} nếu chưa có.
     */
    public Salary getSalaryByMonth(long employeeId, int month, int year) {
        validateMonthYear(month, year);
        return appDb.salaryDao().findByEmployeeAndMonth(employeeId, month, year);
    }

    /**
     * Lấy danh sách bảng lương của tất cả nhân viên trong một tháng và năm cụ thể.
     *
     * <p>Chỉ dành cho quản trị viên (ADMIN).</p>
     *
     * @param month tháng (1–12).
     * @param year  năm.
     * @return danh sách bảng lương trong tháng.
     */
    public List<Salary> getSalariesByMonth(int month, int year) {
        authorization.requireAdmin();
        validateMonthYear(month, year);
        return appDb.salaryDao().findByMonth(month, year);
    }

    /**
     * Xác nhận chốt thanh toán bảng lương (chuyển trạng thái từ DRAFT sang PAID).
     *
     * <p>Điều kiện để chốt lương:
     * <ul>
     *     <li>Người thao tác phải có quyền ADMIN.</li>
     *     <li>Bảng lương tồn tại và đang ở trạng thái DRAFT.</li>
     *     <li>Kỳ lương mục tiêu đã kết thúc theo múi giờ Việt Nam (tháng/năm nhỏ hơn tháng hiện tại).</li>
     *     <li>Không tồn tại bất kỳ bản ghi chấm công OPEN nào của nhân viên trong kỳ lương đó.</li>
     * </ul>
     * </p>
     *
     * @param salaryId ID của bảng lương cần chốt.
     * @return đối tượng {@link Salary} sau khi đã chuyển sang trạng thái PAID.
     */
    public Salary confirmSalaryPaid(long salaryId) {
        authorization.requireAdmin();

        return runInTransaction(() -> {
            Salary salary = appDb.salaryDao().findById(salaryId);
            if (salary == null) {
                throw new IllegalArgumentException("Không tìm thấy bảng lương với ID: " + salaryId);
            }

            if (Constants.STATUS_PAID.equals(salary.status)) {
                throw new IllegalStateException("Bảng lương đã thanh toán, không thể chốt lại");
            }

            if (!Constants.STATUS_DRAFT.equals(salary.status)) {
                throw new IllegalStateException("Bảng lương không ở trạng thái DRAFT: " + salary.status);
            }

            YearMonth targetMonth = YearMonth.of(salary.year, salary.month);
            YearMonth currentMonth = YearMonth.now(clock.withZone(zoneId));

            if (!targetMonth.isBefore(currentMonth)) {
                throw new IllegalStateException("Chỉ có thể chốt lương của tháng đã kết thúc");
            }

            String monthKey = formatMonthKey(salary.year, salary.month);
            WorkLog openLog = appDb.workLogDao().findOpenByEmployeeAndMonth(salary.employeeId, monthKey);
            if (openLog != null) {
                throw new IllegalStateException("Tháng này còn lượt chấm công chưa hoàn tất");
            }

            salary.status = Constants.STATUS_PAID;
            appDb.salaryDao().update(salary);
            return salary;
        });
    }

    /**
     * Phân rã giờ làm việc và tiền lương từ danh sách chấm công và đơn giá giờ.
     */
    public SalaryBreakdown calculateSalaryBreakdown(List<WorkLog> logs, long hourlyRate) {
        return calculator.calculateSalaryBreakdown(logs, hourlyRate);
    }

    private void validateMonthYear(int month, int year) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Tháng phải từ 1 đến 12");
        }
        if (year <= 0) {
            throw new IllegalArgumentException("Năm phải là số dương hợp lệ");
        }
    }

    private String formatMonthKey(int year, int month) {
        return String.format(Locale.ROOT, "%04d-%02d", year, month);
    }

    private <T> T runInTransaction(Callable<T> action) {
        try {
            return appDb.runInTransaction(action);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi trong giao dịch cơ sở dữ liệu: " + e.getMessage(), e);
        }
    }
}
