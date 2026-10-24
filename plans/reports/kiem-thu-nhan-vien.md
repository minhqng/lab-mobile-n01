# Bàn giao kiểm thử quản lý nhân viên TV2

Ngày 24/10/2026. Nhánh `tinh-nang/quan-ly-nhan-vien`, nền `f9ad7ab`.

## Phạm vi và trạng thái

| Task | Trạng thái | Bằng chứng |
|---|---|---|
| NV-01 | Hoàn thành | Giữ Entity/schema, tái sử dụng DAO CRUD; tìm kiếm Unicode và ký tự thường được kiểm tra bằng Room |
| NV-02 | Hoàn thành phần độc lập | RecyclerView, tìm kiếm, tải/rỗng/lỗi/thử lại; bài UI xác minh dữ liệu và `50.000 đ/giờ` |
| NV-03 | Hoàn thành phần độc lập | Form thêm/sửa, validation, giữ ID/accountId; lỗi trùng không đóng form; xoay khi lưu không ghi lặp |
| NV-04 | Chưa hoàn thành tích hợp Auth | Transaction tạo/xóa, quyền và rollback đã đạt; chưa có PasswordHasher/session production của TV1 |
| NV-05 | Hoàn thành kiểm thử độc lập | 16 unit mới + 25 Service/Room mới + 8 UI mới; giữ toàn bộ 17 kiểm thử nền tảng |

## Lệnh và kết quả thực nghiệm

Đặt `ANDROID_HOME=C:/Users/admin/AppData/Local/Android/Sdk` cho tiến trình chạy Gradle. Không commit `local.properties`. Dùng JDK Corretto 21.0.12, Java source/target 17 theo cấu hình dự án, Gradle 9.6.0, SDK 36, Build-Tools 36.0.0.

1. `gradlew.bat testDebugUnitTest --tests '*EmployeeValidatorTest' --no-daemon`: 16/16 đạt.
2. `gradlew.bat clean assembleDebug testDebugUnitTest lintDebug --no-daemon`: thành công.
3. `gradlew.bat connectedDebugAndroidTest --no-daemon`: lượt đầu 39/39 đạt.
4. `gradlew.bat assembleDebug testDebugUnitTest lintDebug connectedDebugAndroidTest --no-daemon`: bản cuối **26/26 unit và 40/40 instrumented đạt**, 0 thất bại, 0 bỏ qua. Lượt cuối có thêm bài lỗi trùng trên form.
5. Sau khi đổi vị trí ảnh kiểm thử sang `files/` nội bộ để đọc bằng `run-as`: `gradlew.bat assembleDebug assembleDebugAndroidTest lintDebug --no-daemon` thành công; cài hai APK bằng `adb install -r`, chạy lại toàn bộ bằng `adb shell am instrument -w -r com.example.employeemanager.test/androidx.test.runner.AndroidJUnitRunner`: **OK (40 tests)**. Đã lấy và kiểm tra trực quan ba PNG thật. Lượt riêng UI trước đó cũng đạt **8/8**. Các lượt chạy lại không cộng dồn vào tổng 66 bài khác nhau.

Máy ảo Pixel_4, Android 11/API 30, serial `emulator-5554`, cổng 5554. Khởi động riêng cho công việc này, không sử dụng thiết bị người dùng. Không có kiểm thử thiết bị thật hoặc Android 36.

Bằng chứng máy tạo, tái tạo bằng các lệnh trên (không commit thư mục build):

- `app/build/test-results/testDebugUnitTest/TEST-*.xml`: 5 schema + 5 constants + 16 validator.
- `app/build/reports/androidTests/connected/debug/index.html`: tổng 40; 7 nền tảng, 25 EmployeeService, 8 EmployeeUi.
- `app/build/reports/lint-results-debug.xml`: 0 lỗi, 10 cảnh báo hiện có về phiên bản dependency/API, backup và icon/theme nền tảng. Không tắt lint hoặc bỏ kiểm tra.
- `app/build/outputs/apk/debug/app-debug.apk`: APK debug biên dịch thành công.
- `git diff -- app/schemas`: không thay đổi schema; không nâng database version hoặc sửa Gradle.

Lượt kiểm tra ban đầu đã thất bại do build chạy trong lúc mã/resource đang được tạo; lượt tiếp theo phát hiện pipe PowerShell làm mất dấu tiếng Việt trong resource. Đã ghi lại UTF-8 đúng và chạy lại thành công. Không che các lỗi ban đầu bằng cách tắt kiểm tra.

## Các hành vi được xác minh

- Chuẩn hóa mã bằng `Locale.ROOT`, email/username chữ thường; giữ dấu họ tên, độ dài 2–100; số điện thoại đúng 10 chữ số bắt đầu 0; đơn giá nguyên dương tới `Long.MAX_VALUE`, từ chối tràn/âm/0/ký tự không phải số.
- Tạo đúng Account role EMPLOYEE và Employee liên kết; không cho trùng code/email/username, Account không tồn tại hoặc một Account dùng cho hai nhân viên.
- Cập nhật giữ ID/accountId, không đổi username/hash/role. Truy vấn ID không tồn tại báo lỗi.
- Tìm mã/tên/email, Unicode tiếng Việt hoa/thường, từ khóa rỗng/null, thứ tự ổn định, `%`/`_` và chuỗi SQL đều là văn bản thường.
- Xóa Employee trước Account trong transaction; có công hoặc lương thì từ chối. Trigger SQLite ép lỗi insert Employee hoặc delete Account để chứng minh rollback bằng dữ liệu thật.
- Hai yêu cầu tạo đồng thời chỉ để lại đúng một cặp Account–Employee.
- Không session/EMPLOYEE không gọi được nghiệp vụ Admin; EMPLOYEE chỉ đọc hồ sơ bản thân. Role/Account bị đổi/xóa trong DB bị kiểm tra lại.
- Giao diện hiển thị đúng tiền, tìm kiếm và trạng thái rỗng, form báo lỗi tại trường, sửa lưu dữ liệu, xóa yêu cầu xác nhận và làm mới danh sách. Khi lưu lỗi trùng, nội dung form giữ nguyên.
- ViewModel giữ kết quả thao tác ghi qua xoay màn hình. Bài regression chặn tại bước hash, recreate, xác nhận Lưu còn khóa, rồi nhả tác vụ và kiểm tra chỉ có một hồ sơ.

## Ảnh thực nghiệm

Ảnh được vẽ từ View thật trên máy ảo trong APK kiểm thử, dùng dữ liệu giả và adapter Auth chỉ có trong kiểm thử. Không phải ảnh đăng nhập production. APK kiểm thử không được phát hành cùng ứng dụng.

- [Danh sách](anh-nhan-vien/nv-danh-sach.png)
- [Biểu mẫu và lỗi nhập liệu](anh-nhan-vien/nv-bieu-mau.png)
- [Chi tiết](anh-nhan-vien/nv-chi-tiet.png)

## Rà soát và giới hạn

Rà soát phát hiện kết quả lưu/xóa bị bỏ khi Activity tạm dừng; đã sửa bằng `EmployeeMutation` giữ trạng thái trong ViewModel, bỏ listener khi tạm dừng và chỉ giao kết quả sau lần kiểm tra quyền tiếp theo. Rà soát lại không phát hiện vấn đề mới cụ thể trong phạm vi này. Kiểm thử xoay màn hình đã đạt.

Auth giả chỉ tồn tại trong `src/androidTest`; không khẳng định hash giả là bảo mật. Production mặc định từ chối tất cả truy cập khi chưa cấu hình TV1; không có tài khoản/quyền Admin giả, bộ băm cạnh tranh hoặc lưu plaintext. `EmployeeAuth` là ranh giới tin cậy mà TV1 phải nối bằng phiên thật và PasswordHasher thật. Cần làm hai việc còn lại: nối Auth/điều hướng thật, rồi kiểm thử E2E sau đăng nhập. Vì vậy chỉ mở PR nháp, chưa sẵn sàng merge đầy đủ.

Tìm kiếm lọc trong bộ nhớ cho danh sách nhỏ, không phân trang, không tìm không dấu hay chuẩn hóa Unicode tương đương. Trạng thái ViewModel giữ qua xoay/tạm dừng, không hứa phục hồi kết quả thao tác sau khi hệ điều hành hủy toàn bộ tiến trình; dữ liệu Room đã commit vẫn tồn tại.

Các điểm có thể xung đột khi tích hợp: `AndroidManifest.xml`, `res/values/strings.xml`, `README.md`. Các thay đổi Manifest chỉ đăng ký ba Activity nội bộ; không đổi launcher. Prefix resource là `nv_`. Không sửa `docs/DAC_TA_DU_AN.md`.

## GitHub

Đã push nhánh `tinh-nang/quan-ly-nhan-vien` và mở [PR nháp #2](https://github.com/minhqng/lab-mobile-n01/pull/2), base `main`. Các commit triển khai: `0d2a1b0` (nghiệp vụ), `4cbee7e` (giao diện), `fcc6158` (kiểm thử), `83f4e83` (hướng dẫn và ảnh). Commit bàn giao sau PR chỉ cập nhật trạng thái kế hoạch/nhật ký. Không force push hoặc merge.
