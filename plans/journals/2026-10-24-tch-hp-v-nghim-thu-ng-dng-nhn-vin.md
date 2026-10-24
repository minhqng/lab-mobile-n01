---
title: Tích hợp và nghiệm thu ứng dụng nhân viên
date: 2026-10-24
summary: "Hoàn thiện liên thông, báo cáo và kiểm thử toàn dự án"
---

# Tích hợp và nghiệm thu ứng dụng nhân viên

## Diễn biến

Đọc đặc tả và khảo sát GitHub; tích hợp Auth, Employee, Attendance và Salary bằng merge giữ nguyên tác giả. TV5 chưa có mã riêng, vì vậy phần báo cáo và dashboard được bổ sung trong phiên tích hợp này.

Hai nguồn phiên cũ khiến module không liên thông. Đã nối adapter về SessionManager, đặt chấm công trong Room transaction và kiểm tra quyền khi vào Activity lẫn trong Service. Tính báo cáo từ giờ vào/ra, dùng cùng quy tắc phút với lương, từ chối dữ liệu trạng thái hỏng và tràn tiền.

Trong quá trình thực hiện có lỗi resource khi merge đang dở, so khớp chữ hoa/thường trong test giao diện và cú pháp finally của hàm lưu ảnh kiểm thử. Đã sửa nguyên nhân và chạy lại; không bỏ hoặc làm yếu bài kiểm thử. CLI ghi phần thân nhật ký sai mã hóa từ pipe PowerShell; đã sửa lại UTF-8 và Việt hóa dòng ghi chú tự động trước khi commit.

## Bằng chứng

Build sạch cuối thành công: 75 JVM, 66 Android trên Pixel_4 API 30, không thất bại hoặc bỏ qua; lint 0 lỗi và 54 cảnh báo. E2E dùng đăng nhập/PBKDF2 thật, chờ 61 giây để chấm công ra, tính/chốt 9.100.000 đồng và xem lương cá nhân. Đã kiểm tra thêm phiên và dữ liệu qua am force-stop, sau đó phục hồi dữ liệu demo và đăng xuất. Bốn ảnh View thật lưu tại plans/reports/anh-nghiem-thu.

## Việc còn lại

Đã push nhánh tích hợp, hoàn thiện PR #5 và mời minhqng review. Bản clone cục bộ mới build và chạy 75 JVM thành công. Không tự giả lập phê duyệt hoặc đóng góp TV5. Thiết bị vật lý chưa kiểm chứng; giới hạn ngoại tuyến, một ca/ngày, không sửa công và không lưu lịch sử đơn giá được giữ theo đặc tả.

> Đây là lịch sử công việc; README và đặc tả sở hữu hướng dẫn và quyết định hiện hành.