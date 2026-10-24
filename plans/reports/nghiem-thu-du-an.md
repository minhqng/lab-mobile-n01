# Báo cáo nghiệm thu dự án

Đây là bản ghi trạng thái của đợt tích hợp hiện tại, không thay thế mã nguồn, kiểm thử hoặc [đặc tả dự án](../../docs/DAC_TA_DU_AN.md). Bằng chứng hành vi nằm trong mã và các bài kiểm thử; bảng dưới đây chỉ định tuyến đến nơi kiểm tra và ghi rõ giới hạn còn lại.

## Tổng hợp bằng chứng

Ngày nghiệm thu: 24/10/2026. Đã chạy `gradlew.bat clean assembleDebug testDebugUnitTest lintDebug connectedDebugAndroidTest --no-daemon`, thành công sau 4 phút 30 giây, 79/79 tác vụ được thực thi.

| Kiểm tra | Tổng | Đạt | Thất bại | Bỏ qua |
|---|---:|---:|---:|---:|
| JVM | 75 | 75 | 0 | 0 |
| Android trên Pixel_4, Android 11, API 30 | 66 | 66 | 0 | 0 |
| Dịch vụ tích hợp thật (nhóm con của Android) | 8 | 8 | 0 | 0 |
| Giao diện tích hợp thật (nhóm con của Android) | 1 | 1 | 0 | 0 |

Tổng 141 bài khác nhau. Lint: 0 lỗi, 54 cảnh báo. Không bỏ qua hoặc vô hiệu hóa bài kiểm thử. Cảnh báo gồm dependency/SDK mới hơn, tài nguyên chưa dùng, icon, kiểu nút, sao lưu, SharedPreferences.commit và notifyDataSetChanged; cấu hình đã khóa được giữ nguyên. Các lỗi ở lượt thử trước (resource đang xung đột, so khớp chữ hoa/thường trong test UI, cú pháp finally của hàm lưu ảnh kiểm thử) đã được sửa và không còn trong lượt cuối.

APK: `app/build/outputs/apk/debug/app-debug.apk`.

SHA-256: `BC7ED99DA8A9EAEA40D5DC2D529D999227A5DE64A63A00422A2FBCA6A6A142DC`.

Bằng chứng máy tạo nằm tại `app/build/test-results/testDebugUnitTest/`, `app/build/outputs/androidTest-results/connected/debug/` và `app/build/reports/lint-results-debug.txt`. Các đầu ra build không được commit; có thể tái tạo bằng lệnh trên.

Đã rà soát độc lập các luồng quyền, transaction, vòng đời Activity và tính tiền; không còn lỗi chặn được xác nhận trong phạm vi mã đã đọc. Chưa kiểm chứng thiết bị vật lý.

## Ảnh giao diện thật

- [Tổng quan quản trị](anh-nghiem-thu/nghiem-thu-quan-tri.png).
- [Chấm công theo thời gian thật](anh-nghiem-thu/nghiem-thu-cham-cong.png).
- [Lương 180 giờ, OT 4 giờ, 9.100.000 đồng đã chốt](anh-nghiem-thu/nghiem-thu-luong-da-chot.png).
- [Lương cá nhân đã thanh toán](anh-nghiem-thu/nghiem-thu-luong-ca-nhan.png).

Ảnh lấy từ View thật bằng Canvas trong APK kiểm thử, đã xem lại trực quan; dữ liệu nhân viên hoàn toàn là dữ liệu mẫu.

## Ma trận KT01–KT30

| Mã | Kết luận hiện tại | Bằng chứng chính | Khoảng trống hoặc giới hạn |
|---|---|---|---|
| KT01 | Đã kiểm chứng | `IntegratedWorkflowTest`, `IntegratedUiTest`, `AuthInstrumentedTest`; đăng nhập ADMIN vào dashboard | Không có |
| KT02 | Đã kiểm chứng | `IntegratedWorkflowTest`, `IntegratedUiTest`, `AuthInstrumentedTest`; đăng nhập EMPLOYEE vào dashboard | Không có |
| KT03 | Đã kiểm chứng | `AuthServiceAndSessionManagerTest`, `IntegratedWorkflowTest`; sai thông tin bị từ chối | Không có |
| KT04 | Đã kiểm chứng trên máy ảo | `SessionManager` đối chiếu Room; workflow tạo `SessionManager` mới; UI mở lại `MainActivity` khôi phục phiên | Đã dùng am force-stop rồi mở lại, giữ đúng phiên ADMIN |
| KT05 | Đã kiểm chứng | `AuthServiceAndSessionManagerTest`, `IntegratedUiTest`; logout rồi mở màn hình lương trực tiếp bị từ chối | Chưa có kiểm thử thiết bị vật lý |
| KT06 | Đã kiểm chứng | `EmployeeServiceTest`, `EmployeeUiTest`, `IntegratedWorkflowTest`, `IntegratedUiTest`; tạo cặp Employee–Account thật trong giao dịch | Không có |
| KT07 | Đã kiểm chứng | `EmployeeServiceTest`, `AppDatabaseTest`; trùng mã/email/username bị từ chối và không để account mồ côi | Không có |
| KT08 | Đã kiểm chứng | `EmployeeServiceTest`, `EmployeeUiTest`; sửa thông tin và đơn giá rồi tải lại danh sách | Không có |
| KT09 | Đã kiểm chứng | `EmployeeServiceTest`, `EmployeeUiTest`; tìm mã, tên, email và xử lý hoa thường tiếng Việt | Không có |
| KT10 | Đã kiểm chứng | `EmployeeServiceTest`, `EmployeeUiTest`; xóa nhân viên chưa có lịch sử cùng account liên kết | Không có |
| KT11 | Đã kiểm chứng | `EmployeeServiceTest`, `AppDatabaseTest`; xóa nhân viên có WorkLog hoặc Salary bị từ chối | Không có |
| KT12 | Đã kiểm chứng | `AttendanceServiceTest`, `AttendanceDatabaseTest`, `IntegratedWorkflowTest`, `IntegratedUiTest`; tạo `OPEN` thật | Không có |
| KT13 | Đã kiểm chứng | `AttendanceServiceTest`, `AppDatabaseTest`, kiểm thử đồng thời trong `IntegratedWorkflowTest`; không tạo bản ghi trùng | Không có |
| KT14 | Đã kiểm chứng | `AttendanceServiceTest`; check-out khi thiếu check-in bị từ chối | Không có |
| KT15 | Đã kiểm chứng | `AttendanceServiceTest`, `AttendanceDatabaseTest`, `IntegratedWorkflowTest`; phiên 8 giờ chuyển `COMPLETED` | Không có |
| KT16 | Đã kiểm chứng | `AttendanceServiceTest`; giờ ra không hợp lệ hoặc qua ngày bị từ chối | Không có |
| KT17 | Đã kiểm chứng | `AttendanceServiceTest`, `IntegratedWorkflowTest`; `OPEN` tháng trước chặn check-in mới | Không có |
| KT18 | Đã kiểm chứng | `AttendanceServiceTest`, `IntegratedUiTest`, `ReportService`; lịch sử và tổng kết lọc theo nhân viên hiện tại | Không có |
| KT19 | Đã kiểm chứng | `WorkHourCalculatorTest`, `SalaryCalculatorTest`; 10 giờ tách 8 giờ thường và 2 giờ tăng ca | Không có |
| KT20 | Đã kiểm chứng | `SalaryCalculatorTest`, `SalaryServiceAndroidTest`, `IntegratedWorkflowTest`, `IntegratedUiTest`; 180 giờ, 4 giờ OT, 9.100.000 đồng | Không có |
| KT21 | Đã kiểm chứng | `SalaryServiceAndroidTest`; tháng không có WorkLog tạo `DRAFT` bằng 0 | Không có |
| KT22 | Đã kiểm chứng | `SalaryServiceAndroidTest`, `IntegratedWorkflowTest`; `OPEN` chặn tính và chốt lương | Không có |
| KT23 | Đã kiểm chứng | `SalaryServiceAndroidTest`, `IntegratedWorkflowTest`, `IntegratedUiTest`; tính lại cập nhật đúng bản ghi cũ | Không có |
| KT24 | Đã kiểm chứng | `SalaryServiceAndroidTest`, `IntegratedWorkflowTest`, `IntegratedUiTest`; kỳ đã kết thúc chuyển sang `PAID` | Không có |
| KT25 | Đã kiểm chứng | `SalaryServiceAndroidTest`, `IntegratedWorkflowTest`; `PAID` và WorkLog trong kỳ bị khóa | Không có |
| KT26 | Đã kiểm chứng | `ReportService`, `IntegratedWorkflowTest`, `IntegratedUiTest`; nhân viên không chọn được lương hoặc công của người khác | Không có |
| KT27 | Đã kiểm chứng | `ReportService`, `IntegratedWorkflowTest`; dashboard và báo cáo dùng cùng dữ liệu `COMPLETED` | Không có |
| KT28 | Đã kiểm chứng | `IntegratedWorkflowTest`, `IntegratedUiTest`, `MySalaryActivity`; chưa có bảng lương chỉ hiển thị trạng thái thiếu dữ liệu, không tự ghi | Không có |
| KT29 | Đã kiểm chứng trên máy ảo | `SessionManager`, `AuthInstrumentedTest`, `IntegratedUiTest`; Room và SharedPreferences giữ dữ liệu khi tạo lại Activity | Sau am force-stop, số điện thoại vừa sửa vẫn còn; chưa có thiết bị vật lý |
| KT30 | Đã kiểm chứng trên máy ảo | `assembleDebug`, `lintDebug`, `IntegratedUiTest` đạt trên emulator; APK debug đã tạo | Build sạch và 66 bài Android đã đạt; chưa có thiết bị vật lý |

## Đối chiếu nguồn và việc còn lại

- Hành vi phiên do [`SessionManager.java`](../../app/src/main/java/com/example/employeemanager/auth/SessionManager.java) sở hữu; seed và chính sách không tái tạo nhân viên đã xóa do [`DataInitializer.java`](../../app/src/main/java/com/example/employeemanager/data/DataInitializer.java) sở hữu.
- Quy tắc `DRAFT`/`PAID`, kiểm tra kỳ đã kết thúc và chặn `OPEN` do [`SalaryService.java`](../../app/src/main/java/com/example/employeemanager/salary/SalaryService.java) sở hữu; báo cáo và dashboard do gói `reports` sở hữu.
- Bốn Entity và schema version 1 không khác origin/main. Giữ đúng Account, Employee, WorkLog, Salary; các query/DTO mới không thêm bảng.
- [PR #5](https://github.com/minhqng/lab-mobile-n01/pull/5) đã sẵn sàng review, nhánh đã push; đã mời minhqng. PR gốc #1–#4 còn mở riêng, toàn bộ commit gốc đã nằm trong nhánh tích hợp.

## Kiểm tra tiến trình và dữ liệu bền vững

Cài APK cuối lên Pixel_4, đăng nhập ADMIN bằng giao diện thật, sửa số điện thoại của NV001 thành dữ liệu mẫu khác. Thực hiện adb shell am force-stop com.example.employeemanager rồi mở MainActivity: ứng dụng vào thẳng dashboard ADMIN, danh sách/chi tiết nhân viên vẫn chứa đúng số mới. Sau đó sửa lại số demo ban đầu và đăng xuất. Đây là một kiểm tra bổ sung qua ADB, không cộng vào 141 bài JVM/Android đã đếm.

Không còn lỗi chức năng đã biết trong phạm vi kiểm tra này. Giới hạn còn lại: thiết bị vật lý chưa chạy; Git chưa có đóng góp riêng của TV5, phần báo cáo do phiên tích hợp bổ sung; main còn chờ thành viên khác review đúng quy trình.

## Bản clone sạch và mã đã kiểm chứng

Đã clone cục bộ bằng git clone --local --no-hardlinks vào thư mục mới, không mang file build/local.properties sang. Chạy assembleDebug testDebugUnitTest --no-daemon thành công sau 25 giây, 39/39 tác vụ thực thi và 75 JVM đạt. Kiểm tra này dùng bộ dependency/SDK đã cài trên máy; chưa mô phỏng máy hoàn toàn không có cache hoặc tải mới mọi dependency từ Internet.

Cây app đã kiểm chứng (git rev-parse HEAD:app): 013c546cbeee2b511b2f0e4570d7412f5e090cb1. Các commit bàn giao sau kiểm thử chỉ thay tài liệu; cây app giữ nguyên.

## Git và đóng góp

Bốn merge giữ nguyên lịch sử: 242ff23 (Auth), 4604a6c (Employee), 854dee0 (Attendance), 81a07dc (Salary). Các commit triển khai: 1b76a37 (phiên/xác thực), 7cc4ae4 (tiền lương/dữ liệu), 3363f17 (báo cáo/điều hướng), 7a7e9db (kiểm thử), d27bb16 (tài liệu/ảnh). Cập nhật trạng thái bàn giao được ghi bằng commit tài liệu tiếp theo.

Nhánh tich-hop/hoan-thien-du-an đã push. PR #5 mở, không phải bản nháp và hướng vào main. Đã yêu cầu minhqng review. Dù main hiện không bật bảo vệ nhánh, vẫn giữ yêu cầu ít nhất một người khác review theo đặc tả mục 13.2; không tự merge hoặc tự giả lập phê duyệt. Bốn nhánh gốc là tổ tiên của HEAD, không force push hoặc xóa nhánh thành viên. Chưa có mã riêng từ TV5 nên không xác nhận đủ năm tác giả.
