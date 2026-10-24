package com.example.employeemanager.integration;

import static org.junit.Assert.*;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import com.example.employeemanager.MainActivity;
import com.example.employeemanager.R;
import com.example.employeemanager.auth.*;
import com.example.employeemanager.attendance.*;
import com.example.employeemanager.data.*;
import com.example.employeemanager.data.entities.*;
import com.example.employeemanager.employee.*;
import com.example.employeemanager.reports.*;
import com.example.employeemanager.salary.*;
import com.example.employeemanager.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.time.*;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

/** Thao tác View thật, không thay Auth/Clock production; dữ liệu tháng trước chỉ là fixture kiểm thử. */
@RunWith(AndroidJUnit4.class)
public class IntegratedUiTest {
    private Instrumentation instrumentation;
    private AppDB db;
    private SessionManager sessions;
    private YearMonth previous;
    private static final String USERNAME = "e2e-final";
    private static final String PASSWORD = "NghiemThu@2026!";
    @Before public void setup() {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        db = AppDB.getInstance(instrumentation.getTargetContext());
        DataInitializer.initializeInitialData(db);
        sessions = new SessionManager(instrumentation.getTargetContext(), db);
        sessions.logout();
        EmployeeModule.configure(new EmployeeAuthAdapter(sessions));
        previous = YearMonth.now(DateTimeUtils.systemClock()).minusMonths(1);
    }
    @After public void cleanup() {
        instrumentation.runOnMainSync(() -> {
            for (Stage stage : Stage.values()) {
                if (stage == Stage.DESTROYED) continue;
                for (Activity activity : new ArrayList<>(ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(stage))) {
                    if (!activity.isFinishing()) activity.finish();
                }
            }
        });
        instrumentation.waitForIdleSync();
        Employee employee = db.employeeDao().findByCode("E2E-FINAL");
        if (employee != null) {
            db.runInTransaction(() -> {
                for (Salary salary : db.salaryDao().findByEmployeeId(employee.id)) db.salaryDao().delete(salary);
                for (String key : new String[]{previous.toString(), YearMonth.now(DateTimeUtils.systemClock()).toString()})
                    for (WorkLog log : db.workLogDao().findByEmployeeAndMonth(employee.id, key)) db.workLogDao().delete(log);
                db.employeeDao().delete(employee);
                db.accountDao().delete(db.accountDao().findById(employee.accountId));
            });
        }
        sessions.logout();
    }
    private void awaitUi(BooleanSupplier condition) {
        long deadline = SystemClock.uptimeMillis() + 20000;
        AtomicBoolean ready = new AtomicBoolean();
        do {
            instrumentation.runOnMainSync(() -> ready.set(condition.getAsBoolean()));
            if (ready.get()) return;
            SystemClock.sleep(50);
        } while (SystemClock.uptimeMillis() < deadline);
        fail("Giao diện chưa đạt trạng thái nghiệm thu trong thời gian chờ");
    }
    private Activity current() {
        AtomicReference<Activity> result = new AtomicReference<>();
        instrumentation.runOnMainSync(() -> {
            for (Activity activity : ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)) result.set(activity);
        });
        return result.get();
    }
    private Activity screen(Class<? extends Activity> type) {
        awaitUi(() -> {
            for (Activity activity : ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)) {
                if (type.isInstance(activity)) return true;
            }
            return false;
        });
        return current();
    }
    private void click(Activity activity, int id) {
        awaitUi(() -> activity.findViewById(id).isEnabled());
        instrumentation.runOnMainSync(() -> activity.findViewById(id).performClick());
    }
    private void input(Activity activity, int id, String value) {
        instrumentation.runOnMainSync(() -> ((EditText) activity.findViewById(id)).setText(value));
    }
    private boolean contains(Activity activity, int id, String text) {
        return ((TextView) activity.findViewById(id)).getText().toString().contains(text);
    }
    private Activity login(String username, String password, Class<? extends Activity> dashboard) {
        Activity login = screen(LoginActivity.class);
        input(login, R.id.auth_et_username, username);
        input(login, R.id.auth_et_password, password);
        click(login, R.id.auth_btn_login);
        Activity result = screen(dashboard);
        awaitUi(() -> result.findViewById(android.R.id.content).getVisibility() == android.view.View.VISIBLE
                && !((TextView) result.findViewById(R.id.bc_content)).getText().toString().isEmpty());
        return result;
    }
    private void finish(Activity activity) { instrumentation.runOnMainSync(activity::finish); }
    private void period(Activity activity, int monthId, int yearId) {
        int currentYear = YearMonth.now(DateTimeUtils.systemClock()).getYear();
        instrumentation.runOnMainSync(() -> {
            ((Spinner) activity.findViewById(monthId)).setSelection(previous.getMonthValue() - 1);
            ((Spinner) activity.findViewById(yearId)).setSelection(previous.getYear() - currentYear + 2);
        });
    }
    private void capture(Activity activity, String name) {
        instrumentation.runOnMainSync(() -> {
            android.view.View view = activity.getWindow().getDecorView();
            Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
            view.draw(new Canvas(bitmap));
            try {
            try (FileOutputStream stream = new FileOutputStream(new File(activity.getFilesDir(), name + ".png"))) {
                assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream));
            } catch (java.io.IOException error) { throw new AssertionError(error); }
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                android.content.ContentValues values = new android.content.ContentValues();
                values.put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, name + ".png");
                values.put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png");
                values.put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/lab-mobile-n01");
                android.content.ContentResolver resolver = activity.getContentResolver();
                android.net.Uri uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                assertNotNull("Phải lưu được ảnh nghiệm thu trên máy ảo", uri);
                try (java.io.OutputStream output = resolver.openOutputStream(uri)) {
                    assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output));
                } catch (java.io.IOException error) { throw new AssertionError(error); }
            }
            } finally { bitmap.recycle(); }
        });
    }

    @Test public void dangNhapTaoNhanVienChamCongChotLuongVaBaoCao() throws Exception {
        instrumentation.startActivitySync(new Intent(instrumentation.getTargetContext(), MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        Activity admin = login("admin", DataInitializer.DEMO_ADMIN_RAW_PASSWORD, AdminDashboardActivity.class);
        capture(admin, "nghiem-thu-quan-tri");
        click(admin, R.id.bc_employees);
        Activity list = screen(EmployeeListActivity.class);
        click(list, R.id.nv_add);
        Activity form = screen(EmployeeFormActivity.class);
        awaitUi(() -> form.findViewById(R.id.nv_root).getVisibility() == android.view.View.VISIBLE);
        input(form, R.id.nv_code, "E2E-FINAL"); input(form, R.id.nv_fullname, "Nhân viên nghiệm thu");
        input(form, R.id.nv_email, "e2e-final@example.com"); input(form, R.id.nv_phone, "0912345678");
        input(form, R.id.nv_hourly_rate, "50000"); input(form, R.id.nv_username, USERNAME);
        input(form, R.id.nv_password, PASSWORD); input(form, R.id.nv_confirmation, PASSWORD);
        click(form, R.id.nv_save);
        screen(EmployeeListActivity.class);
        Employee employee = db.employeeDao().findByCode("E2E-FINAL");
        assertNotNull("Biểu mẫu phải tạo Employee và Account thật", employee);
        assertTrue(PasswordHasher.verifyPassword(PASSWORD, db.accountDao().findById(employee.accountId).password));
        input(list, R.id.nv_keyword, "E2E-FINAL");
        awaitUi(() -> ((RecyclerView) list.findViewById(R.id.nv_list)).getAdapter().getItemCount() == 1);
        finish(list); admin = screen(AdminDashboardActivity.class);
        click(admin, R.id.bc_logout);
        Activity worker = login(USERNAME, PASSWORD, EmployeeDashboardActivity.class);
        click(worker, R.id.bc_attendance);
        Activity attendance = screen(AttendanceActivity.class);
        click(attendance, R.id.buttonCheckIn);
        long enteredAt = SystemClock.elapsedRealtime();
        awaitUi(() -> contains(attendance, R.id.textAttendanceStatus, "Đang"));
        String today = DateTimeUtils.todayString(DateTimeUtils.systemClock());
        assertEquals(Constants.STATUS_OPEN, db.workLogDao().findByEmployeeAndDate(employee.id, today).status);
        assertFalse(sessions.getValidatedSession().isAdmin());
        click(attendance, R.id.buttonWorkHistory);
        Activity history = screen(WorkHistoryActivity.class);
        awaitUi(() -> ((RecyclerView) history.findViewById(R.id.recyclerWorkHistory)).getAdapter().getItemCount() == 1);
        finish(history); screen(AttendanceActivity.class);
        while (SystemClock.elapsedRealtime() - enteredAt < 61000) SystemClock.sleep(200);
        click(attendance, R.id.buttonCheckOut);
        awaitUi(() -> contains(attendance, R.id.textAttendanceStatus, "Đã hoàn tất"));
        assertEquals(Constants.STATUS_COMPLETED, db.workLogDao().findByEmployeeAndDate(employee.id, today).status);
        capture(attendance, "nghiem-thu-cham-cong");
        finish(attendance); worker = screen(EmployeeDashboardActivity.class);
        for (int day = 1; day <= 22; day++) db.workLogDao().insert(new WorkLog(employee.id,
                previous.atDay(day).toString(), "08:00:00", day <= 20 ? "16:00:00" : "18:00:00",
                day <= 20 ? 8 : 10, Constants.STATUS_COMPLETED));
        click(worker, R.id.bc_summary);
        Activity summary = screen(MonthlySummaryActivity.class); period(summary, R.id.bc_month, R.id.bc_year);
        click(summary, R.id.bc_load); awaitUi(() -> contains(summary, R.id.bc_content, "180"));
        finish(summary); worker = screen(EmployeeDashboardActivity.class);
        click(worker, R.id.bc_salary);
        Activity mySalary = screen(MySalaryActivity.class); period(mySalary, R.id.bc_month, R.id.bc_year);
        click(mySalary, R.id.bc_load); awaitUi(() -> contains(mySalary, R.id.bc_content, "Chưa có bảng lương"));
        assertEquals(0, db.salaryDao().countByEmployeeId(employee.id));
        finish(mySalary); worker = screen(EmployeeDashboardActivity.class); click(worker, R.id.bc_logout);
        admin = login("admin", DataInitializer.DEMO_ADMIN_RAW_PASSWORD, AdminDashboardActivity.class);
        click(admin, R.id.bc_payroll);
        Activity payroll = screen(SalaryListActivity.class);
        awaitUi(() -> ((Spinner) payroll.findViewById(R.id.spEmployee)).getAdapter() != null);
        period(payroll, R.id.spMonth, R.id.spYear);
        awaitUi(() -> payroll.findViewById(R.id.btnCalculateSalary).isEnabled());
        instrumentation.runOnMainSync(() -> {
            Spinner spinner = payroll.findViewById(R.id.spEmployee);
            for (int i = 0; i < spinner.getCount(); i++) if (spinner.getItemAtPosition(i).toString().contains("E2E-FINAL")) spinner.setSelection(i);
        });
        click(payroll, R.id.btnCalculateSalary);
        awaitUi(() -> ((RecyclerView) payroll.findViewById(R.id.rvSalaryList)).getAdapter().getItemCount() == 1);
        Salary draft = db.salaryDao().findByEmployeeAndMonth(employee.id, previous.getMonthValue(), previous.getYear());
        assertEquals(9100000, draft.totalSalary);
        instrumentation.runOnMainSync(() -> ((RecyclerView) payroll.findViewById(R.id.rvSalaryList)).findViewHolderForAdapterPosition(0).itemView.performClick());
        Activity detail = screen(SalaryDetailActivity.class);
        awaitUi(() -> contains(detail, R.id.tvDetailTotalSalary, "9.100.000"));
        click(detail, R.id.btnRecalculate);
        awaitUi(() -> detail.findViewById(R.id.btnConfirmPaid).isEnabled());
        assertEquals(draft.id, db.salaryDao().findByEmployeeAndMonth(employee.id, previous.getMonthValue(), previous.getYear()).id);
        click(detail, R.id.btnConfirmPaid);
        java.lang.reflect.Field field = SalaryDetailActivity.class.getDeclaredField("paidDialog"); field.setAccessible(true);
        instrumentation.runOnMainSync(() -> {
            try { ((AlertDialog) field.get(detail)).getButton(AlertDialog.BUTTON_POSITIVE).performClick(); }
            catch (IllegalAccessException error) { throw new AssertionError(error); }
        });
        awaitUi(() -> contains(detail, R.id.tvDetailStatus, "Đã thanh toán"));
        capture(detail, "nghiem-thu-luong-da-chot");
        finish(detail); screen(SalaryListActivity.class); finish(payroll);
        admin = screen(AdminDashboardActivity.class); click(admin, R.id.bc_logout);
        worker = login(USERNAME, PASSWORD, EmployeeDashboardActivity.class);
        click(worker, R.id.bc_salary);
        Activity personal = screen(MySalaryActivity.class); period(personal, R.id.bc_month, R.id.bc_year); click(personal, R.id.bc_load);
        awaitUi(() -> contains(personal, R.id.bc_content, "9.100.000") && contains(personal, R.id.bc_content, "Đã thanh toán"));
        capture(personal, "nghiem-thu-luong-ca-nhan");
        finish(personal); worker = screen(EmployeeDashboardActivity.class);
        finish(worker);
        instrumentation.startActivitySync(new Intent(instrumentation.getTargetContext(), MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        worker = screen(EmployeeDashboardActivity.class);
        assertEquals(employee.id, sessions.requireCurrentEmployeeId());
        click(worker, R.id.bc_logout); screen(LoginActivity.class);
        Activity denied = instrumentation.startActivitySync(new Intent(instrumentation.getTargetContext(), SalaryListActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        awaitUi(denied::isFinishing); screen(LoginActivity.class);
        assertNull(sessions.getValidatedSession());
        instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);
        instrumentation.startActivitySync(new Intent(instrumentation.getTargetContext(), MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        screen(LoginActivity.class);
        assertNull(sessions.getValidatedSession());
    }
}
