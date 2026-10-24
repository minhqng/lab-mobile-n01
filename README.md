# Ứng dụng quản lý thời gian làm việc và tiền lương nhân viên

## 1. Tên đề tài

**Ứng dụng quản lý thời gian làm việc và tiền lương nhân viên** — bài thực hành Android của nhóm 5 thành viên.

## 2. Giới thiệu

Ứng dụng giúp quản trị viên quản lý nhân viên, theo dõi công và bảng lương; giúp nhân viên tự chấm công, xem lịch sử làm việc và tiền lương của chính mình. Ứng dụng hoạt động hoàn toàn ngoại tuyến trên một thiết bị, dữ liệu được lưu cục bộ bằng Room/SQLite, không có máy chủ và không đồng bộ giữa nhiều điện thoại.

Phiên bản hiện tại gồm **nền tảng kỹ thuật (NEN-01) và module quản lý nhân viên (TV2)**. Module nhân viên đã có nghiệp vụ, giao diện và kiểm thử độc lập; vẫn chờ Authentication của TV1 để sử dụng từ luồng đăng nhập thật. Launcher hiện giữ nguyên màn hình nền tảng.

## 3. Tính năng chính

### 3.1. Trạng thái hoàn thành thực tế

**Đã hoàn thành trong giai đoạn nền tảng:**

- Khung dự án Android Java + XML, Gradle Wrapper và cấu hình build thống nhất.
- Room `2.8.5` với đúng bốn Entity: `Account`, `Employee`, `WorkLog`, `Salary`.
- Năm DAO: `AccountDao`, `EmployeeDao`, `WorkLogDao`, `SalaryDao`, `ReportDao`.
- `AppDB` singleton dùng Application Context, không bật truy vấn trên luồng giao diện.
- Hằng số dùng chung trong `Constants` và bộ thực thi nền dùng chung trong `AppExecutors`.
- Màn hình khởi động tối thiểu hiển thị tiếng Việt.
- Kiểm thử hợp đồng schema và kiểm thử ràng buộc cơ sở dữ liệu đã chạy thực tế.

**Đã triển khai trong module nhân viên:**

- Danh sách RecyclerView, tìm kiếm theo mã/họ tên/email, trạng thái tải/rỗng/lỗi và thử lại.
- Biểu mẫu thêm/sửa, lỗi tại trường nhập, kiểm tra đơn giá nguyên dương và tràn miền `long`.
- Chi tiết, xác nhận xóa, giao dịch tạo/xóa Account–Employee; chặn xóa khi có công hoặc lương.
- Service kiểm tra lại Account và role từ Room; nhân viên chỉ đọc hồ sơ của chính tài khoản phiên.
- Giữ kết quả lưu/xóa qua xoay màn hình bằng ViewModel; truy vấn dùng `AppExecutors` ngoài luồng giao diện.
- Không đổi bốn Entity, schema/version, cấu hình Gradle, launcher hoặc nghiệp vụ các module khác.

**Chưa triển khai (sẽ thực hiện ở các task sau):**

- Đăng nhập, phân quyền, lưu phiên bằng SharedPreferences và tài khoản Admin mẫu.
- Adapter Authentication thật cho module nhân viên: mặc định từ chối truy cập, không cấp quyền Admin giả và không tạo tài khoản khi chưa có bộ băm hợp lệ.
- Chấm công vào/ra và lịch sử công.
- Tính lương, tăng ca và chốt bảng lương `PAID`.
- Dashboard và các màn hình báo cáo.

Không có chức năng nào trong nhóm “chưa triển khai” được mô tả là đã hoạt động.

### 3.2. Chức năng dự kiến của Quản trị viên (ADMIN)

- Đăng nhập và đăng xuất.
- Xem, tìm kiếm danh sách nhân viên theo mã, họ tên hoặc email.
- Thêm, sửa, xóa nhân viên và mức lương theo giờ.
- Tạo tài khoản `EMPLOYEE` gắn với nhân viên trong cùng một giao dịch.
- Xem lịch sử công, tính lương tháng và chốt trạng thái `PAID`.
- Xem tổng quan số nhân viên, tổng giờ làm và tổng lương.

### 3.3. Chức năng dự kiến của Nhân viên (EMPLOYEE)

- Đăng nhập và đăng xuất.
- Xem thông tin cá nhân và trạng thái chấm công trong ngày.
- Chấm công vào/ra đúng quy tắc.
- Xem lịch sử công, tổng giờ và giờ tăng ca theo tháng.
- Xem bảng lương của chính mình, không xem được dữ liệu của người khác.

## 4. Công nghệ sử dụng

| Thành phần | Lựa chọn |
|---|---|
| Ngôn ngữ | Java |
| Giao diện | Android Views + XML Layout |
| Cơ sở dữ liệu | Room `2.8.5` trên nền SQLite |
| Xử lý annotation | `annotationProcessor` |
| Phiên đăng nhập (giai đoạn sau) | SharedPreferences `MODE_PRIVATE` |
| Xử lý bất đồng bộ | `ExecutorService` dùng chung |
| Danh sách | RecyclerView |
| Build | Gradle Wrapper `9.6.0`, Android Gradle Plugin `9.4.1` |
| Quản lý mã nguồn | Git + GitHub |

Không sử dụng Kotlin, Jetpack Compose, Firebase, KSP hoặc Room 3.

## 5. Cấu trúc dự án

```text
lab-mobile-n01/
├── README.md
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradlew, gradlew.bat
├── gradle/wrapper/
├── docs/
│   └── DAC_TA_DU_AN.md
└── app/
    ├── build.gradle
    ├── proguard-rules.pro
    ├── schemas/
    │   └── com.example.employeemanager.data.AppDB/1.json
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── java/com/example/employeemanager/
        │   │   ├── MainActivity.java
        │   │   ├── data/
        │   │   │   ├── AppDB.java
        │   │   │   ├── entities/
        │   │   │   │   ├── Account.java
        │   │   │   │   ├── Employee.java
        │   │   │   │   ├── WorkLog.java
        │   │   │   │   └── Salary.java
        │   │   │   ├── dao/
        │   │   │   │   ├── AccountDao.java
        │   │   │   │   ├── EmployeeDao.java
        │   │   │   │   ├── WorkLogDao.java
        │   │   │   │   ├── SalaryDao.java
        │   │   │   │   └── ReportDao.java
        │   │   │   └── dto/
        │   │   └── util/
        │   │       ├── Constants.java
        │   │       └── AppExecutors.java
        │   └── res/
        │       ├── layout/activity_main.xml
        │       └── values/{colors,strings,themes}.xml
        ├── test/java/.../SchemaContractTest.java, ConstantsTest.java
        └── androidTest/java/.../AppDatabaseTest.java
```

Package `employee` hiện chứa Service, DTO, validator, hợp đồng Auth, ba Activity, adapter và ViewModel giữ thao tác ghi. Các package `auth`, `attendance`, `salary`, `reports` và `data/dto` sẽ được tạo khi có lớp thực sự cần thiết. Không tạo tệp rỗng chỉ để đủ cây thư mục.

## 6. Yêu cầu môi trường

- Android Studio phiên bản có hỗ trợ Android Gradle Plugin `9.4.1` (đã kiểm chứng với Android Studio `2026.2.1`).
- JDK 17 trở lên; cấu hình đã kiểm chứng dùng JDK 25 đi kèm Android Studio.
- Android SDK Platform 36 và Build-Tools 36.0.0.
- Gradle `9.6.0` được tự động tải qua Gradle Wrapper.
- `minSdk 26`, `compileSdk 36`, `targetSdk 36`.
- Kết nối Internet ở lần build đầu để tải dependency AndroidX và Room.

## 7. Hướng dẫn chạy

1. Clone repository:

   ```bash
   git clone https://github.com/minhqng/lab-mobile-n01.git
   cd lab-mobile-n01
   ```

2. Mở thư mục gốc bằng Android Studio và chờ quá trình đồng bộ Gradle hoàn tất.
3. Kiểm tra `local.properties` có đường dẫn SDK đúng với máy, ví dụ:

   ```properties
   sdk.dir=/home/ten-nguoi-dung/Android/Sdk
   ```

   Tệp này không được commit lên Git.

4. Chọn một máy ảo hoặc thiết bị thật có Android 8.0 (API 26) trở lên.
5. Bấm **Run** để cài và mở ứng dụng.

Có thể build và kiểm thử bằng dòng lệnh tại thư mục gốc:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
```

APK debug được tạo tại `app/build/outputs/apk/debug/app-debug.apk`.

### 7.1. Tích hợp module nhân viên với TV1

TV1 triển khai `employee.EmployeeAuth` và gọi `EmployeeModule.configure(adapter)` một lần trong `Application.onCreate()` trước khi mở màn hình nhân viên:

- `long currentAccountId()`: lấy ID từ phiên đã xác thực; trả `0` khi không có phiên. Không nhận ID/role từ Intent và không cache role riêng. Service vẫn đọc lại Account trong Room trước khi cấp quyền.
- `String hashPassword(String plainPassword)`: gọi `PasswordHasher` thật của TV1, trả chuỗi băm chậm có salt theo đặc tả; báo lỗi nếu bộ băm chưa sẵn sàng. Không lưu hoặc ghi nhật ký mật khẩu nhập. Các callback này được gọi trên luồng nền.
- Mặc định chưa cấu hình: ID phiên bằng `0`, từ chối mọi truy cập; bộ băm mặc định báo chức năng chưa sẵn sàng. Không có SharedPreferences, tài khoản Admin hay thuật toán băm thay thế trong module TV2.

Sau khi nối Auth, màn hình quản trị mở `EmployeeListActivity` bằng Intent nội bộ. Từ danh sách chọn thêm mới hoặc mở chi tiết để sửa/xóa. Các màn hình dùng khóa Intent `EmployeeActivity.EXTRA_EMPLOYEE_ID`; không chuyển Account/password/hash qua Intent. Tất cả Activity TV2 đặt `exported="false"` và kiểm tra quyền lại khi quay về màn hình.

API Service giữ sáu phương thức tại mục 10.2 đặc tả. `CreateEmployeeRequest` nhận `code, fullname, email, phone, hourlyRate, username, password`; `UpdateEmployeeRequest` nhận năm trường đầu. `hourlyRate` trong request là chuỗi để kiểm tra input trước khi chuyển sang `long` trong Entity. Kết quả đọc không tồn tại báo `EmployeeException`; UI ánh xạ `messageRes` và `field` sang lỗi tiếng Việt. Các hàm Service chạy đồng bộ trên worker.

Tìm kiếm trim từ khóa, đổi chữ thường bằng `Locale.ROOT`, lọc chuỗi con trên danh sách DAO đã sắp tên/mã. Hỗ trợ hoa/thường tiếng Việt, giữ nguyên dấu, coi `%` và `_` là ký tự thường; không tìm không dấu hoặc chuẩn hóa các cách mã hóa Unicode tương đương. Giải pháp lọc trong bộ nhớ phù hợp danh sách nhỏ của bài thực hành, chưa phân trang cho dữ liệu lớn.

Chạy riêng kiểm thử màn hình trên máy ảo bằng `gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.employeemanager.employee.EmployeeUiTest`. Auth/bộ băm giả chỉ nằm trong APK kiểm thử; dữ liệu kiểm thử có tiền tố `TV2-UI-`, được dọn sau mỗi bài. Ảnh giao diện do bài kiểm thử vẽ từ View thật được lưu trong thư mục `files/` nội bộ của ứng dụng với tên `nv-danh-sach.png`, `nv-bieu-mau.png`, `nv-chi-tiet.png`. Khi chạy Gradle, runner có thể gỡ APK và xóa ảnh sau kiểm thử; muốn lấy ảnh, cài hai APK rồi chạy `adb shell am instrument -w -e class com.example.employeemanager.employee.EmployeeUiTest com.example.employeemanager.test/androidx.test.runner.AndroidJUnitRunner` và đọc tệp qua `adb exec-out run-as com.example.employeemanager cat files/<tên-ảnh>.png`. Lưu stdout theo dạng nhị phân. Đây không phải bằng chứng đăng nhập tích hợp thật.

## 8. Tài khoản dùng thử

**Hiện tại chưa có tài khoản demo trong cơ sở dữ liệu.** Việc seed tài khoản Admin và nhân viên mẫu thuộc task DN-01 (đăng nhập), sẽ được bổ sung khi `PasswordHasher` và `AuthService` hoàn thành. Không có mật khẩu nào được lưu dạng rõ trong mã nguồn, Git, SharedPreferences hoặc Logcat.

Bảng dưới đây là tài khoản **dự kiến** theo đặc tả, chỉ dùng cho môi trường demo ngoại tuyến và **chưa sử dụng được ở phiên bản nền tảng này**:

| Vai trò | Tên đăng nhập dự kiến | Mật khẩu nhập trên màn hình demo |
|---|---|---|
| Quản trị viên | `admin` | `QuanLy@2026!` |
| Nhân viên mẫu | `nv001` | `NhanVien@2026!` |

Khi được triển khai, cột `Account.password` chỉ lưu chuỗi băm PBKDF2 có salt; mật khẩu trong bảng chỉ là giá trị người dùng nhập để đăng nhập. Các thông tin này không phù hợp cho môi trường thật.

## 9. Quy tắc chấm công và tính lương

- Mỗi nhân viên chỉ có tối đa một phiên chấm công trong một ngày; ca không qua nửa đêm.
- Ngày làm việc và giờ được xác định theo múi giờ `Asia/Ho_Chi_Minh`, không phụ thuộc múi giờ tùy ý của thiết bị.
- Ngày lưu dạng `yyyy-MM-dd`, giờ lưu dạng `HH:mm:ss`; giao diện hiển thị ngày `dd/MM/yyyy`, giờ `HH:mm`.
- Một ngày làm việc chuẩn là **480 phút (8 giờ)**. Phần vượt quá 480 phút được tính là tăng ca với hệ số **1,5**.
- Tổng giờ trong tháng bằng tổng số phút làm việc của các ngày `COMPLETED` chia cho 60; giờ tăng ca đã nằm trong tổng giờ.
- Tiền lương được tính bằng số phút nguyên và `BigDecimal`, làm tròn `HALF_UP` tới đồng; không dùng `double` để tính tiền.
- Phiên `OPEN` chưa hoàn tất không được tính vào tổng giờ trả lương và không cho tính hoặc chốt lương tháng đó.
- Trạng thái bảng lương: `DRAFT` được tính lại và cập nhật cùng bản ghi; `PAID` bị khóa, không tính lại và không ghi thêm công trong tháng.
- Chỉ được chốt `PAID` cho tháng đã kết thúc.
- Không tính nghỉ trưa, ngày lễ hoặc cuối tuần; đây là quy tắc giả định phục vụ bài thực hành.

## 10. Hướng dẫn thao tác theo vai trò

### 10.1. Luồng Quản trị viên (dự kiến)

1. Đăng nhập bằng tài khoản `ADMIN`.
2. Mở danh sách nhân viên, tìm theo mã, họ tên hoặc email.
3. Thêm nhân viên mới kèm tài khoản `EMPLOYEE`, mã nhân viên và đơn giá giờ.
4. Mở chi tiết nhân viên để sửa hoặc xóa khi chưa có lịch sử công/lương.
5. Chọn tháng/năm, tính lương, xem giờ thường, giờ tăng ca và tổng tiền.
6. Chốt bảng lương `PAID` khi tháng đã kết thúc và không còn phiên `OPEN`.

### 10.2. Luồng Nhân viên (dự kiến)

1. Đăng nhập bằng tài khoản `EMPLOYEE`.
2. Xem trạng thái chấm công trong ngày và thông tin cá nhân.
3. Chấm công vào; khi đủ điều kiện thì chấm công ra.
4. Xem lịch sử công và tổng kết tháng của chính mình.
5. Xem bảng lương tháng của chính mình.

Luồng đăng nhập và điều hướng theo vai trò **chưa hoạt động**. Giao diện/nghiệp vụ quản lý nhân viên đã có và được kiểm thử qua adapter chỉ dành cho kiểm thử; muốn thao tác từ ứng dụng thật cần hoàn thiện mục 7.1. Màn hình khởi động vẫn chỉ thông báo nền tảng đã được khởi tạo.

## 11. Phân công 5 thành viên

| Thành viên | Nhóm công việc | Task chính |
|---|---|---|
| TV1 | Nền tảng, đăng nhập, phiên, tích hợp | NEN-01, DN-01, DN-02, DN-03, DN-04 |
| TV2 | Quản lý nhân viên | NV-01, NV-02, NV-03, NV-04, NV-05 |
| TV3 | Chấm công | CC-01, CC-02, CC-03, CC-04, CC-05 |
| TV4 | Tính lương | LG-01, LG-02, LG-03, LG-04, LG-05 |
| TV5 | Báo cáo và kiểm thử | BC-01, BC-02, BC-03, KT-01, KT-02 |

Mỗi thành viên phụ trách code, giao diện và kiểm thử của module mình, tạo nhánh riêng từ `main` và gửi Pull Request bằng tiếng Việt.

## 12. Kiểm thử và kết quả thực tế

Các lệnh sau đã được chạy thật trong quá trình bàn giao nền tảng:

| Lệnh | Kết quả |
|---|---|
| `./gradlew assembleDebug` | Thành công, tạo `app-debug.apk` |
| `./gradlew testDebugUnitTest` | Thành công, **10/10** bài kiểm thử đạt |
| `./gradlew connectedDebugAndroidTest` | Thành công trên máy ảo Pixel_8 (Android 15, API 35), **7/7** bài kiểm thử đạt |
| `./gradlew lintDebug` | Thành công, **0 lỗi** và 10 cảnh báo về phiên bản thư viện, biểu tượng và cấu hình sao lưu |
| Cài APK và mở `MainActivity` | Ứng dụng mở bình thường, không có lỗi trong bộ nhớ log sự cố |

Nội dung đã kiểm chứng:

- Room annotation processor biên dịch thành công và xuất schema tại `app/schemas/com.example.employeemanager.data.AppDB/1.json`.
- Đúng bốn bảng nghiệp vụ, đúng tên bảng và tên cột.
- Khóa chính tự tăng; các chỉ mục UNIQUE trên `Account.username`, `Employee.code`, `Employee.email`, `Employee.accountId`, cặp (`WorkLog.employeeId`, `WorkLog.workDate`) và bộ ba (`Salary.employeeId`, `Salary.month`, `Salary.year`).
- Khóa ngoại `Employee.accountId → Account.id`, `WorkLog.employeeId → Employee.id`, `Salary.employeeId → Employee.id` đều dùng `ON DELETE RESTRICT`.
- Chèn trùng khóa UNIQUE bị SQLite từ chối; xóa tài khoản còn nhân viên hoặc xóa nhân viên còn lịch sử bị từ chối.
- Phiên `OPEN` không được tính vào truy vấn tổng giờ đã hoàn tất.

Phần chưa kiểm chứng:

- Chưa chạy trên thiết bị thật; kết quả instrumented test chỉ từ máy ảo Pixel_8 API 35.
- Chưa kiểm thử đăng nhập, chấm công, tính lương và luồng nhân viên sau đăng nhập thật. Kết quả TV2 được cập nhật bên dưới; không sử dụng bộ băm giả trong production.

Không có bài kiểm thử nào bị bỏ qua, bỏ assert hoặc tắt để có kết quả xanh.

### 12.1. Kết quả module nhân viên TV2 — 24/10/2026

Đã chạy `gradlew.bat clean assembleDebug testDebugUnitTest lintDebug`, sau đó chạy lại `gradlew.bat assembleDebug testDebugUnitTest lintDebug connectedDebugAndroidTest` trên bản mã cuối:

| Kiểm tra | Kết quả thực tế |
|---|---|
| APK debug và Room annotation processor | Thành công; schema `1.json` giữ nguyên |
| Kiểm thử đơn vị | **26/26** đạt: 10 nền tảng + 16 validation nhân viên |
| Kiểm thử trên Pixel_4 Android 11, API 30 | **40/40** đạt: 7 nền tảng + 25 Service/Room + 8 giao diện |
| Lint | **0 lỗi**, 10 cảnh báo phần nền tảng |
| Kiểm thử bỏ qua | **0** |

Đã kiểm tra CRUD, chuẩn hóa, trùng dữ liệu, tìm kiếm chữ hoa/thường tiếng Việt và ký tự `%`/`_`, kiểm tra quyền trực tiếp ở Service, quyền đọc hồ sơ bản thân, xóa có lịch sử, rollback tạo/xóa hai bảng bằng trigger SQLite, tạo trùng đồng thời và xoay màn hình trong lúc lưu. Kiểm thử giao diện dùng adapter Auth giả trong APK kiểm thử; **chưa kiểm chứng PasswordHasher/session thật hoặc đăng nhập đến màn hình quản trị**.

## 13. Giới hạn của phiên bản

- Ứng dụng ngoại tuyến, dữ liệu chỉ nằm trên một thiết bị; không có backend, Firebase, REST API hoặc đồng bộ nhiều máy.
- Không có đăng ký tài khoản công khai, GPS, sinh trắc học, đa ca trong ngày hoặc xuất PDF/Excel.
- Không tính nghỉ trưa, ngày lễ, cuối tuần hoặc lương theo hợp đồng.
- Một nhân viên chỉ có tối đa một phiên chấm công mỗi ngày; không hỗ trợ ca qua nửa đêm.
- Không có chức năng sửa giờ chấm công thủ công. Phiên `OPEN` bị quên qua ngày cần quy trình xử lý dữ liệu demo riêng; ứng dụng không tự xóa lịch sử.
- Bảng lương `PAID` là bất biến trong phạm vi bài thực hành.
- Đây là mô hình phân quyền của ứng dụng demo cục bộ, không thay thế bảo mật có máy chủ xác thực.

## 14. Tài liệu đặc tả

Tài liệu yêu cầu đầy đủ nằm tại [docs/DAC_TA_DU_AN.md](docs/DAC_TA_DU_AN.md). Đây là nguồn yêu cầu chính thức về nghiệp vụ, cơ sở dữ liệu, phân quyền, giao diện và tiêu chí nghiệm thu của dự án.

## Quy định nhánh, commit và Pull Request

- Nhánh `main` chứa nền tảng ổn định. Mỗi thành viên tạo nhánh từ `main` theo quy ước:
  - `tinh-nang/nen-tang-dang-nhap` (TV1)
  - `tinh-nang/quan-ly-nhan-vien` (TV2)
  - `tinh-nang/cham-cong` (TV3)
  - `tinh-nang/tinh-luong` (TV4)
  - `tinh-nang/bao-cao-kiem-thu` (TV5)
- Không commit `local.properties`, `.gradle/`, `build/`, `app/build/`, khóa bí mật hoặc mật khẩu thật.
- Commit ngắn gọn, 4–12 từ, tiếng Việt có dấu, mô tả một thay đổi logic. Ví dụ: `Thiết lập cơ sở dữ liệu Room`, `Thêm chức năng đăng nhập theo vai trò`.
- Không dùng thông điệp chung chung như `update`, `fix`, `done`, `WIP` hoặc commit tiếng Anh.
- Pull Request phải có tiêu đề và mô tả tiếng Việt, gồm: mục tiêu, thay đổi chính, cách kiểm thử và kết quả thực tế, lưu ý tương thích. Không force push lên `main`.
