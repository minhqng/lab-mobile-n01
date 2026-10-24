# Hoàn thiện tích hợp và nghiệm thu

## Kết quả cần đạt

Hoàn thiện 25 task tại mục 11 đặc tả: đăng nhập, nhân viên, chấm công, lương, báo cáo và nghiệm thu KT01–KT30. Giữ bốn Entity, schema version 1, Java/XML, Room 2.8.5 và lịch sử tác giả. Không thêm backend, bảng nghiệp vụ, chức năng sửa giờ hay thay chính sách lương.

## Các chặng

- [x] Đọc yêu cầu và đặc tả, khảo sát Git và nhánh hiện hữu.
- [x] Tích hợp Auth → Employee → Attendance → Salary, biên dịch sau mỗi module.
- [x] Kết nối Employee/Attendance/Salary với SessionManager duy nhất.
- [x] Hoàn thiện ReportService, DTO, dashboard và điều hướng.
- [x] Kiểm thử JVM, Room, giao diện, E2E; đối chiếu KT01–KT30.
- [ ] Rà soát, cập nhật README, build sạch, push và tạo PR.

## Triển khai và phụ thuộc

Nhánh `tich-hop/hoan-thien-du-an` tạo từ `origin/main` f9ad7ab. PR #3, #2, #1 đang mở khi khảo sát; nhánh TV4 có mã lương; TV5 chưa có khác biệt với main. Tích hợp bằng merge bảo toàn lịch sử. Xử lý các file dùng chung theo cả hai phía. Không sửa đặc tả.

EmployeeAuth lấy accountId từ phiên đã đối chiếu Room và băm qua AuthService. SessionProvider production gọi SessionManager.requireCurrentEmployeeId. SalaryAuthorization gọi SessionManager.requireRole. Báo cáo cá nhân tự suy ra employeeId, tổng công từ giờ COMPLETED và tổng lương phân biệt đã tính/đã thanh toán.

## Kiểm chứng

Chạy assembleDebug sau mỗi merge; cuối cùng clean assembleDebug testDebugUnitTest lintDebug và connectedDebugAndroidTest khi máy ảo sẵn sàng. Giữ toàn bộ test cũ, bổ sung kiểm thử tích hợp thật, chứng minh 180 giờ/4 giờ OT/9.100.000 đồng, DRAFT cùng ID, khóa PAID, cô lập dữ liệu và logout. Kết quả chi tiết lưu tại `plans/reports/`.

## Rủi ro và phục hồi

Không reset/force push/xóa nhánh thành viên. Merge có commit riêng để truy vết và có thể revert khi cần. Không thay schema hoặc xóa dữ liệu ứng dụng người dùng. Dữ liệu nghiệm thu nằm trong database kiểm thử; không mở đường sửa giờ production. Không tự giả lập đóng góp TV5 hoặc phê duyệt GitHub.
