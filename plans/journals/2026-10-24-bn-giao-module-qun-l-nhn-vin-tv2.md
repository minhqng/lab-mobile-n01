---
title: Bàn giao module quản lý nhân viên TV2
date: 2026-10-24
summary: "Kiểm chứng nghiệp vụ, giao diện và giao dịch; mở PR nháp chờ Auth TV1"
---

# Bàn giao module quản lý nhân viên TV2

## Kết quả

Đã triển khai TV2 trên nhánh tinh-nang/quan-ly-nhan-vien: Service, validator, DAO tìm kiếm Unicode, ba màn hình Java/XML, giao dịch Account–Employee và kiểm thử. Đã push và mở PR nháp https://github.com/minhqng/lab-mobile-n01/pull/2, không merge.

## Sự cố và cách sửa

Pipe PowerShell sang Node từng làm mất dấu resource; đã ghi UTF-8 trực tiếp và build lại. Rà soát phát hiện mất callback lưu/xóa khi pause/rotation; đã thêm ViewModel giữ trạng thái ghi và regression test chặn tại phép băm rồi recreate. Ảnh test ban đầu không lấy được sau runner gỡ ứng dụng; đã chuyển ảnh sang files/ nội bộ, chạy trực tiếp instrumentation và đọc nhị phân qua run-as. Git chưa có danh tính; đã cấu hình riêng repository bằng tài khoản GitHub đang xác thực và địa chỉ noreply. Tác nhân Git chuyên biệt không khởi động được do model không được hỗ trợ; đã push trực tiếp và xác minh remote.

## Bằng chứng và quyết định

26/26 unit và 40/40 instrumented đạt; lint 0 lỗi, 10 cảnh báo nền tảng; APK build thành công, schema giữ nguyên. Lượt trực tiếp sau cập nhật vị trí ảnh cũng OK (40 tests). Đã xem ba PNG thật. Chi tiết nằm tại plans/reports/kiem-thu-nhan-vien.md; nhật ký không thay thế README hoặc đặc tả.

## Việc còn lại

TV1 nối EmployeeAuth bằng session và PasswordHasher thật rồi chạy E2E đăng nhập. Production hiện từ chối mặc định, không có Auth/hash/Admin giả. Máy ảo Pixel_4 cổng 5554 được khởi động riêng và được dừng khi bàn giao. Không xuất bản AgentWiki; nhật ký giữ cục bộ trong repository.

> Historical work record — not durable authority. Prefer docs/specs/ADRs for current decisions.
