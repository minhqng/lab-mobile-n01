# Triển khai quản lý nhân viên TV2

- Mục tiêu: NV-01 đến NV-05, CRUD nhân viên, giao diện Java/XML, giao dịch Account–Employee và kiểm thử dữ liệu thực.
- Ràng buộc: giữ bốn bảng, schema/version 1, Gradle và API công khai; không sửa Authentication, chấm công, tính lương hay launcher.
- Ngoài phạm vi: dashboard, đổi mật khẩu, xóa lịch sử, hệ thống xác thực thay thế.
- Nghiệm thu: build/test/lint chạy thật; CRUD, chuẩn hóa, tìm kiếm, quyền và rollback có bằng chứng; commit/push nhánh TV2 và PR nháp nếu Auth chưa sẵn sàng.

## Các bước và phụ thuộc

1. [x] Đọc đặc tả, khảo sát foundation, đồng bộ main và tạo nhánh TV2.
2. [x] NV-01/NV-04: giữ Entity; hoàn thiện tìm kiếm; Service, validation, DTO, quyền và giao dịch độc lập. Auth production còn chờ TV1.
3. [x] NV-02/NV-03: danh sách, form, chi tiết, bất đồng bộ, vòng đời và resource tiếng Việt.
4. [x] NV-05: 26/26 unit, 40/40 instrumented trên API 30; build thành công; lint 0 lỗi/10 cảnh báo. Rà soát đã sửa và kiểm chứng lỗi kết quả ghi qua xoay màn hình.
5. [x] README, báo cáo thực nghiệm, commit và push nhánh TV2; đã mở [PR nháp #2](https://github.com/minhqng/lab-mobile-n01/pull/2), không merge.

## Quyết định tích hợp

Main tại thời điểm khảo sát là `f9ad7ab`, chưa có package Auth. Cung cấp hợp đồng lấy ID phiên và băm mật khẩu để TV1 nối từ Application; mặc định từ chối. Service đọc lại Account và role từ Room, chỉ cho nhân viên đọc hồ sơ của chính tài khoản phiên. Không tạo SharedPreferences hoặc PasswordHasher riêng.

## Kiểm tra và hoàn tác

Chạy test validation trước, sau đó clean/assembleDebug/testDebugUnitTest/lintDebug và connectedDebugAndroidTest nếu thiết bị khả dụng. Giữ schema xuất không đổi. Dùng database trong bộ nhớ và trigger kiểm thử để chứng minh rollback. Hoàn tác bằng revert các commit TV2; không cần migration vì không đổi schema.

## Việc chờ tích hợp

- [ ] TV1 cung cấp adapter `EmployeeAuth` từ phiên thật và `PasswordHasher`, cấu hình trong Application, thêm điều hướng nội bộ từ màn hình quản trị.
- [ ] Chạy nghiệm thu đăng nhập ADMIN/EMPLOYEE thật và tạo Account dùng bộ băm production.

Chi tiết bằng chứng tại [báo cáo kiểm thử](reports/kiem-thu-nhan-vien.md).
