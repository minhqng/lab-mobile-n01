# Ứng dụng quản lý thời gian làm việc và tiền lương nhân viên

## 1. Tên đề tài

Ứng dụng quản lý thời gian làm việc và tiền lương nhân viên — ứng dụng Android Java/XML cho bài thực hành của nhóm năm thành viên.

## 2. Giới thiệu

Ứng dụng phục vụ hai nhóm người dùng: quản trị viên quản lý nhân viên và bảng lương; nhân viên tự chấm công, xem lịch sử công, tổng kết tháng và bảng lương của chính mình. Ứng dụng chạy ngoại tuyến trên một thiết bị, lưu dữ liệu cục bộ bằng Room/SQLite và không có máy chủ hay đồng bộ nhiều thiết bị.

## 3. Tính năng chính

- **Quản trị viên:** đăng nhập, xem dashboard, thêm/sửa/xóa và tìm kiếm nhân viên, quản lý tài khoản nhân viên, tính lại bảng lương `DRAFT`, xem chi tiết và chốt kỳ đã kết thúc sang `PAID`.
- **Nhân viên:** đăng nhập, khôi phục phiên, chấm công vào/ra, xem lịch sử và tổng kết công theo tháng, xem bảng lương của mình.
- **Phân quyền:** `SessionManager` là nguồn phiên duy nhất; các dịch vụ kiểm tra vai trò và nhân viên hiện tại trước khi đọc hoặc ghi dữ liệu. Các màn hình bảo vệ đều được khai báo nội bộ trong `AndroidManifest.xml`.
- **Báo cáo:** dashboard quản trị cộng công hoàn tất và tách tiền `DRAFT`/`PAID`; báo cáo cá nhân tự suy ra nhân viên từ phiên, không nhận `employeeId` tùy ý từ giao diện.

## 4. Công nghệ sử dụng

| Thành phần | Lựa chọn |
|---|---|
| Ngôn ngữ và giao diện | Java, Android Views, XML |
| Cơ sở dữ liệu | Room `2.8.5` trên SQLite, `annotationProcessor` |
| Phiên đăng nhập | SharedPreferences `MODE_PRIVATE`, đối chiếu Room |
| Mật khẩu | `PBKDF2WithHmacSHA256`, 600.000 vòng, salt 16 byte, khóa 256 bit |
| Tác vụ nền | `ExecutorService` dùng chung |
| Công cụ xây dựng | Gradle Wrapper `9.6.0`, Android Gradle Plugin `9.4.1` |
| Quản lý mã nguồn | Git và GitHub |

Ứng dụng không dùng Kotlin, Jetpack Compose, Firebase, KSP hoặc backend.

## 5. Cấu trúc dự án

```text
lab-mobile-n01/
├── README.md
├── docs/DAC_TA_DU_AN.md
├── app/build.gradle
└── app/src/
    ├── main/java/com/example/employeemanager/
    │   ├── auth/       # đăng nhập, phiên và bảo vệ màn hình
    │   ├── data/       # AppDB, bốn Entity, DAO và khởi tạo dữ liệu
    │   ├── employee/   # quản lý nhân viên
    │   ├── attendance/ # chấm công và lịch sử
    │   ├── salary/     # tính và chốt lương
    │   └── reports/    # dashboard và báo cáo
    ├── main/res/       # giao diện XML và chuỗi tiếng Việt
    ├── test/           # kiểm thử JVM
    └── androidTest/    # kiểm thử Room, dịch vụ và giao diện thật
```

Hợp đồng dữ liệu và chính sách nghiệp vụ nằm trong [đặc tả dự án](docs/DAC_TA_DU_AN.md); mã nguồn, kiểm thử và cấu hình là nguồn sự thật cho hành vi hiện tại.

## 6. Yêu cầu môi trường

- Android Studio có thể dùng Android Gradle Plugin `9.4.1` và Android SDK Platform 36.
- JDK 21 Corretto đã được dùng cho lần kiểm chứng hiện tại; mã nguồn đặt `sourceCompatibility` và `targetCompatibility` ở Java 17.
- Gradle Wrapper `9.6.0`, `minSdk 26`, `compileSdk 36`, `targetSdk 36`.
- Có thể dùng máy ảo Android 11 / API 30. Bộ kiểm chứng hiện tại chạy trên cấu hình Pixel_4 API 30; chưa có kiểm chứng trên thiết bị vật lý.
- Lần xây dựng đầu cần kết nối Internet để tải AndroidX và Room.

## 7. Hướng dẫn chạy

1. Sao chép kho mã và mở thư mục gốc bằng Android Studio.

   ```bash
   git clone https://github.com/minhqng/lab-mobile-n01.git
   cd lab-mobile-n01
   ```

2. Chờ Android Studio đồng bộ Gradle, kiểm tra SDK và chọn máy ảo hoặc thiết bị Android từ API 26 trở lên.
3. Chạy ứng dụng bằng nút **Run**. Khi ứng dụng mở lần đầu trên cơ sở dữ liệu trống, `DataInitializer` tạo dữ liệu demo trong một giao dịch Room.
4. Từ thư mục gốc, dùng Gradle Wrapper khi cần:

   ```bash
   ./gradlew assembleDebug
   ./gradlew testDebugUnitTest
   ./gradlew lintDebug
   ./gradlew connectedDebugAndroidTest
   ```

   Trên Windows dùng `gradlew.bat` tương ứng. Lệnh kiểm thử thiết bị cần emulator hoặc thiết bị đã kết nối.

APK debug được tạo tại `app/build/outputs/apk/debug/app-debug.apk`.

## 8. Tài khoản dùng thử

Các tài khoản sau chỉ dành cho demo ngoại tuyến, không dùng cho môi trường thật:

| Vai trò | Tên đăng nhập | Mật khẩu |
|---|---|---|
| Quản trị viên | `admin` | `QuanLy@2026!` |
| Nhân viên mẫu | `nv001` | `NhanVien@2026!` |

Đây là mật khẩu demo công khai, có trực tiếp trong mã nguồn `DataInitializer` và README để nhập thử. Cơ sở dữ liệu chỉ chứa chuỗi băm PBKDF2 có salt; SharedPreferences không chứa mật khẩu hoặc hash. Không sử dụng tài khoản demo cho dữ liệu thật. Seed chỉ chạy khi bảng Account trống, không tái tạo nhân viên đã được xóa hợp lệ.

## 9. Quy tắc chấm công và tính lương

- Mỗi nhân viên chỉ có một phiên chấm công trong một ngày. `check-in` tạo `OPEN`; chỉ `check-out` hợp lệ mới chuyển sang `COMPLETED`.
- Ngày và giờ nghiệp vụ dùng `Asia/Ho_Chi_Minh`, lưu `yyyy-MM-dd`/`HH:mm:ss`, hiển thị `dd/MM/yyyy`/`HH:mm`. Không trừ giờ nghỉ trưa, ngày lễ hoặc cuối tuần.
- Thời lượng được tính theo phút hoàn chỉnh; 8 giờ đầu là giờ thường, phần vượt quá là tăng ca với hệ số `1,5`. Ca không được đi qua ngày khác và `OPEN` tồn đọng chặn thao tác tiếp theo.
- Tiền dùng số nguyên `long`; phép tính dùng `BigDecimal` với làm tròn `HALF_UP`. Bộ dữ liệu chuẩn 20 ngày 8 giờ và 2 ngày 10 giờ ở mức 50.000 đồng/giờ cho 180 giờ, 4 giờ tăng ca và 9.100.000 đồng.
- Tạo hoặc tính lại lương chỉ dành cho `ADMIN`; bản ghi `DRAFT` được cập nhật trên cùng khóa nhân viên/tháng/năm. Chỉ kỳ đã kết thúc, không còn `OPEN`, mới được chốt `PAID`; `PAID` không được tính lại hoặc sửa.
- Đổi đơn giá chỉ ảnh hưởng lương nháp được tính lại sau đó. Bảng lương đã thanh toán giữ nguyên số tiền; hệ thống không lưu lịch sử đơn giá. Không ghi thêm hoặc sửa công trong kỳ đã thanh toán.

## 10. Hướng dẫn thao tác theo vai trò

**Quản trị viên:** mở ứng dụng → đăng nhập `ADMIN` → xem dashboard → mở quản lý nhân viên để tạo hoặc cập nhật tài khoản → mở quản lý lương để chọn kỳ và nhân viên → tính hoặc tính lại `DRAFT` → mở chi tiết và xác nhận chốt `PAID` cho kỳ đã kết thúc → đăng xuất.

**Nhân viên:** đăng nhập `EMPLOYEE` → xem thông tin cá nhân → mở chấm công để vào/ra → xem lịch sử hoặc tổng kết tháng → mở bảng lương cá nhân. Dữ liệu cá nhân được suy ra từ phiên hiện tại; không chọn nhân viên khác qua giao diện.

Muốn trình diễn bộ công 180 giờ và lương 9.100.000 đồng mà không đợi nhiều ngày, chạy `gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.employeemanager.integration.IntegratedUiTest`. Bài kiểm thử tạo dữ liệu tháng trước trong APK kiểm thử, thao tác giao diện thật, rồi dọn nhân viên nghiệm thu. Không có chức năng sửa giờ hoặc tạo công giả trên giao diện ứng dụng.

## 11. Phân công 5 thành viên

Đặc tả giao 25 task theo năm nhóm sau:

| Thành viên | Nhóm task | Nội dung chính | Dấu vết hợp nhất |
|---|---|---|---|
| TV1 | `NEN-01`, `DN-01`–`DN-04` | nền tảng, xác thực, đăng nhập, phiên và quyền | [PR #3](https://github.com/minhqng/lab-mobile-n01/pull/3), nhánh gốc `3629bf1` |
| TV2 | `NV-01`–`NV-05` | Entity/DAO, danh sách, biểu mẫu, CRUD và giao dịch nhân viên | [PR #2](https://github.com/minhqng/lab-mobile-n01/pull/2), nhánh gốc `688378c` |
| TV3 | `CC-01`–`CC-05` | WorkLog, vào/ra, kiểm tra trùng và lịch sử công | [PR #1](https://github.com/minhqng/lab-mobile-n01/pull/1), nhánh gốc `a21fbdf` |
| TV4 | `LG-01`–`LG-05` | tính giờ, tăng ca, `DRAFT`/`PAID` và màn hình lương | [PR #4](https://github.com/minhqng/lab-mobile-n01/pull/4), nhánh gốc `10f39c9` |
| TV5 | `BC-01`–`BC-03`, `KT-01`–`KT-02` | báo cáo, dashboard, tài liệu và nghiệm thu | Nhánh TV5 trùng `main` tại `f9ad7ab`, không có mã báo cáo nguyên thủy |

Mã từ bốn PR được tích hợp vào nhánh `tich-hop/hoan-thien-du-an` bằng bốn merge commit để giữ tác giả gốc. Báo cáo, dashboard và các kiểm thử tích hợp được bổ sung trong đợt tích hợp hiện tại, không gán ngược cho đóng góp nguyên thủy của TV5. Lịch sử commit gốc của TV2 được giữ nguyên, kể cả một số tiền tố tiếng Anh; không sửa lịch sử chỉ để đổi cách đặt tên.

## 12. Kiểm thử và kết quả thực tế

Đã chạy `gradlew.bat clean assembleDebug testDebugUnitTest lintDebug connectedDebugAndroidTest --no-daemon` trên bản mã cuối, thành công sau 4 phút 30 giây; cả 79 tác vụ đều được thực thi sau clean.

| Kiểm tra | Tổng | Đạt | Thất bại | Bỏ qua |
|---|---:|---:|---:|---:|
| Kiểm thử JVM | 75 | 75 | 0 | 0 |
| Kiểm thử Android trên Pixel_4 Android 11 / API 30 | 66 | 66 | 0 | 0 |
| Trong 66 bài Android: dịch vụ tích hợp thật | 8 | 8 | 0 | 0 |
| Trong 66 bài Android: luồng giao diện tích hợp thật | 1 | 1 | 0 | 0 |

Tổng cộng **141 bài khác nhau**, không cộng lại các nhóm con hoặc các lần chạy lặp. Lint có **0 lỗi, 54 cảnh báo**; không tắt kiểm tra để có kết quả đạt. APK debug đã tạo thành công.

Luồng giao diện dùng PBKDF2 và phiên thật: tạo nhân viên qua biểu mẫu, chấm công theo đồng hồ production và chờ 61 giây, tổng kết 180 giờ/4 giờ tăng ca, tính và tính lại 9.100.000 đồng, xác nhận thanh toán, xem lương cá nhân, mở lại Activity, đăng xuất và kiểm tra Back/màn hình bị bảo vệ. Các ca dịch vụ dùng Clock cố định và Room thật; bộ kiểm thử độc lập TV2 vẫn giữ adapter giả chỉ trong APK kiểm thử.

[Xem ma trận KT01–KT30 và bằng chứng](plans/reports/nghiem-thu-du-an.md). [Ảnh lương đã thanh toán](plans/reports/anh-nghiem-thu/nghiem-thu-luong-da-chot.png) và [ảnh lương cá nhân](plans/reports/anh-nghiem-thu/nghiem-thu-luong-ca-nhan.png) được vẽ từ View thật trên máy ảo. Kiểm tra bổ sung qua ADB đã xác nhận phiên ADMIN và dữ liệu Employee vẫn còn sau `am force-stop` rồi mở lại; đã phục hồi dữ liệu demo và đăng xuất. Chưa kiểm thử trên thiết bị vật lý.

## 13. Giới hạn của phiên bản

Phiên bản hiện tại hoạt động ngoại tuyến trên một thiết bị, không có backend, đồng bộ hoặc tài khoản từ xa. Chính sách không tính nghỉ trưa, không cho giao diện chỉnh công quá hạn và không cho ca chấm công đi qua ngày. Chưa có kiểm chứng trên thiết bị vật lý. Việc hợp nhất vào main cần tuân thủ review của thành viên khác theo đặc tả.

## 14. Tài liệu đặc tả

Đọc [Đặc tả dự án và kế hoạch triển khai](docs/DAC_TA_DU_AN.md) để xem schema, hợp đồng module, chính sách nghiệp vụ, ma trận `KT01`–`KT30`, kịch bản demo và tiêu chí nghiệm thu. Báo cáo bằng chứng trạng thái được lưu tại [plans/reports/nghiem-thu-du-an.md](plans/reports/nghiem-thu-du-an.md).
