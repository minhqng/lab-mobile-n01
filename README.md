# Ứng dụng quản lý thời gian làm việc và tiền lương nhân viên

## 1. Tên đề tài

**Ứng dụng quản lý thời gian làm việc và tiền lương nhân viên** — bài thực hành Android của nhóm 5 thành viên.

## 2. Giới thiệu

Ứng dụng giúp quản trị viên quản lý nhân viên, theo dõi công và bảng lương; giúp nhân viên tự chấm công, xem lịch sử làm việc và tiền lương của chính mình. Ứng dụng hoạt động hoàn toàn ngoại tuyến trên một thiết bị, dữ liệu được lưu cục bộ bằng Room/SQLite, không có máy chủ và không đồng bộ giữa nhiều điện thoại.

Module nhân viên đã được tích hợp cùng đăng nhập; adapter production đang được nối trong chặng tiếp theo. Phiên bản hiện tại đã hoàn thành **nền tảng kỹ thuật (NEN-01)** cùng **mô-đun đăng nhập và phân quyền hai vai trò (DN-01, DN-02, DN-03, DN-04)**: hệ thống xác thực mật khẩu băm bảo mật PBKDF2, quản lý phiên đăng nhập qua SharedPreferences có đối chiếu Room, khởi tạo tài khoản mẫu và giao diện đăng nhập điều phối theo vai trò.

## 3. Tính năng chính

### 3.1. Trạng thái hoàn thành thực tế

**Đã hoàn thành trong giai đoạn nền tảng và đăng nhập (NEN-01, DN-01, DN-02, DN-03, DN-04):**

- Khung dự án Android Java + XML, Gradle Wrapper và cấu hình build thống nhất.
- Room `2.8.5` với đúng bốn Entity: `Account`, `Employee`, `WorkLog`, `Salary`.
- Năm DAO: `AccountDao`, `EmployeeDao`, `WorkLogDao`, `SalaryDao`, `ReportDao`.
- `AppDB` singleton dùng Application Context, không bật truy vấn trên luồng giao diện.
- Hằng số dùng chung trong `Constants` và bộ thực thi nền dùng chung trong `AppExecutors`.
- Cơ chế băm và xác minh mật khẩu an toàn `PBKDF2WithHmacSHA256` kèm `SecureRandom`, 600.000 vòng lặp, salt 16 byte, khóa dẫn xuất 256 bit (`PasswordHasher`).
- Dịch vụ xác thực `AuthService` chuẩn hóa tên đăng nhập, xác thực an toàn thời gian và bảo mật thông báo lỗi tiếng Việt không làm lộ tài khoản.
- Quản lý phiên đăng nhập `SessionManager` lưu thông tin phiên trong SharedPreferences (`MODE_PRIVATE`), bắt buộc đối chiếu trực tiếp với Room và hủy phiên khi tài khoản bị sửa/xóa.
- Kiểm tra phân quyền đa tầng tại Service (`requireRole`, `requireCurrentEmployeeId`), không chỉ ẩn giao diện.
- Khởi tạo dữ liệu ban đầu an toàn và idempotent (`DataInitializer`), tự động tạo tài khoản Quản trị viên và Nhân viên mẫu demo khi cơ sở dữ liệu mới.
- Màn hình đăng nhập `LoginActivity` hỗ trợ kiểm tra dữ liệu, khóa thao tác khi đang xác thực, xóa mật khẩu khi lỗi và hiển thị thông báo tiếng Việt.
- Điều phối vai trò từ `MainActivity`, xóa back stack khi chuyển màn hình hoặc đăng xuất, ngăn nút Back quay lại màn hình được bảo vệ.
- Màn hình điều phối tạm thời cho Quản trị viên (`AdminPlaceholderActivity`) và Nhân viên (`EmployeePlaceholderActivity`) với đầy đủ nút đăng xuất và kiểm tra quyền.
- Bộ 29 bài kiểm thử đơn vị và hợp đồng dữ liệu đạt 100% kết quả thành công.

**Chưa triển khai (sẽ thực hiện ở các task tiếp theo của TV2–TV5):**

- Thêm, sửa, xóa, tìm kiếm nhân viên (TV2 - `tinh-nang/quan-ly-nhan-vien`).
- Chấm công vào/ra và lịch sử công (TV3 - `tinh-nang/cham-cong`).
- Tính lương, tăng ca và chốt bảng lương `PAID` (TV4 - `tinh-nang/tinh-luong`).
- Dashboard quản trị và màn hình báo cáo lương cá nhân (TV5 - `tinh-nang/bao-cao-kiem-thu`).

Không có chức năng nào trong nhóm “chưa triển khai” được mô tả là đã hoạt động.

### 3.2. Chức năng của Quản trị viên (ADMIN)

- Đăng nhập hệ thống bằng tài khoản `ADMIN`.
- Lưu phiên làm việc và tự động khôi phục khi mở lại ứng dụng.
- Đăng xuất an toàn và xóa toàn bộ dữ liệu phiên.
- Được bảo vệ quyền truy cập: không thể bị truy cập bởi tài khoản nhân viên.
- *(Giai đoạn sau)* Xem, tìm kiếm danh sách nhân viên theo mã, họ tên hoặc email.
- *(Giai đoạn sau)* Thêm, sửa, xóa nhân viên và mức lương theo giờ.
- *(Giai đoạn sau)* Tạo tài khoản `EMPLOYEE` gắn với nhân viên trong cùng một giao dịch.
- *(Giai đoạn sau)* Xem lịch sử công, tính lương tháng và chốt trạng thái `PAID`.
- *(Giai đoạn sau)* Xem tổng quan số nhân viên, tổng giờ làm và tổng lương.

### 3.3. Chức năng của Nhân viên (EMPLOYEE)

- Đăng nhập hệ thống bằng tài khoản `EMPLOYEE`.
- Lưu phiên làm việc và tra cứu đúng mã định danh nhân viên (`employeeId`) từ phiên đăng nhập.
- Đăng xuất an toàn và xóa toàn bộ dữ liệu phiên.
- Bị chặn truy cập các chức năng dành riêng cho Quản trị viên ở tầng dịch vụ.
- *(Giai đoạn sau)* Xem thông tin cá nhân và trạng thái chấm công trong ngày.
- *(Giai đoạn sau)* Chấm công vào/ra đúng quy tắc.
- *(Giai đoạn sau)* Xem lịch sử công, tổng giờ và giờ tăng ca theo tháng.
- *(Giai đoạn sau)* Xem bảng lương của chính mình, không xem được dữ liệu của người khác.

## 4. Công nghệ sử dụng

| Thành phần | Lựa chọn |
|---|---|
| Ngôn ngữ | Java |
| Giao diện | Android Views + XML Layout |
| Cơ sở dữ liệu | Room `2.8.5` trên nền SQLite |
| Xử lý annotation | `annotationProcessor` |
| Băm mật khẩu | PBKDF2WithHmacSHA256 (600.000 vòng, salt 16 byte, key 256 bit) |
| Phiên đăng nhập | SharedPreferences `MODE_PRIVATE` + đối chiếu Room |
| Xử lý bất đồng bộ | `ExecutorService` dùng chung (`AppExecutors`) |
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
        │   │   ├── auth/
        │   │   │   ├── PasswordHasher.java
        │   │   │   ├── AuthService.java
        │   │   │   ├── AuthResult.java
        │   │   │   ├── SessionData.java
        │   │   │   ├── SessionManager.java
        │   │   │   ├── LoginActivity.java
        │   │   │   ├── AdminPlaceholderActivity.java
        │   │   │   └── EmployeePlaceholderActivity.java
        │   │   ├── data/
        │   │   │   ├── AppDB.java
        │   │   │   ├── DataInitializer.java
        │   │   │   ├── entities/
        │   │   │   │   ├── Account.java
        │   │   │   │   ├── Employee.java
        │   │   │   │   ├── WorkLog.java
        │   │   │   │   └── Salary.java
        │   │   │   └── dao/
        │   │   │       ├── AccountDao.java
        │   │   │       ├── EmployeeDao.java
        │   │   │       ├── WorkLogDao.java
        │   │   │       ├── SalaryDao.java
        │   │   │       └── ReportDao.java
        │   │   └── util/
        │   │       ├── Constants.java
        │   │       └── AppExecutors.java
        │   └── res/
        │       ├── layout/
        │       │   ├── activity_main.xml
        │       │   ├── auth_activity_login.xml
        │       │   ├── auth_activity_admin_placeholder.xml
        │       │   └── auth_activity_employee_placeholder.xml
        │       └── values/{colors,strings,themes}.xml
        ├── test/java/.../
        │   ├── auth/PasswordHasherTest.java
        │   ├── auth/AuthServiceAndSessionManagerTest.java
        │   ├── data/DataInitializerUnitTest.java
        │   ├── data/SchemaContractTest.java
        │   └── util/ConstantsTest.java
        └── androidTest/java/.../
            ├── auth/AuthInstrumentedTest.java
            └── data/AppDatabaseTest.java
```

## 6. Yêu cầu môi trường

- Android Studio phiên bản có hỗ trợ Android Gradle Plugin `9.4.1` (đã kiểm chứng với Android Studio `2026.2.1`).
- JDK 17 trở lên; cấu hình đã kiểm chứng dùng JDK 25 đi kèm Android Studio (`/opt/android-studio/jbr`).
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
JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug
JAVA_HOME=/opt/android-studio/jbr ./gradlew testDebugUnitTest
JAVA_HOME=/opt/android-studio/jbr ./gradlew lintDebug
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

Khi ứng dụng khởi động lần đầu, tiện ích `DataInitializer` sẽ tự động tạo một tài khoản Quản trị viên và một tài khoản Nhân viên mẫu trong một Room transaction. Mọi mật khẩu được băm bằng `PasswordHasher` trước khi lưu vào Room, tuyệt đối không lưu mật khẩu rõ trong cơ sở dữ liệu hay SharedPreferences.

Các tài khoản dùng thử phục vụ môi trường demo ngoại tuyến:

| Vai trò | Tên đăng nhập | Mật khẩu nhập trên màn hình demo | Ghi chú |
|---|---|---|---|
| Quản trị viên | `admin` | `QuanLy@2026!` | Có toàn quyền quản trị hệ thống |
| Nhân viên mẫu | `nv001` | `NhanVien@2026!` | Liên kết với nhân viên `NV001` (Nguyễn Văn An) |

Cột `Account.password` chỉ lưu chuỗi băm PBKDF2 có salt dạng `pbkdf2_sha256$600000$<salt>$<hash>`. Mật khẩu trong bảng trên chỉ là giá trị người dùng nhập để xác thực. Các thông tin này chỉ dành cho mục đích demo môn học và không phù hợp cho môi trường thật.

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

### 10.1. Luồng Quản trị viên (ADMIN)

1. Mở ứng dụng: nếu chưa đăng nhập, màn hình `LoginActivity` sẽ hiển thị.
2. Nhập tên đăng nhập `admin` và mật khẩu `QuanLy@2026!`, bấm nút **Đăng nhập**.
3. Hệ thống xác thực thông tin, lưu phiên vào SharedPreferences và điều hướng tới màn hình Quản trị viên (`AdminPlaceholderActivity`).
4. Xem thông tin tài khoản và vai trò đã xác thực.
5. Bấm nút **Đăng xuất**: phiên bị xóa hoàn toàn khỏi thiết bị và ứng dụng quay về màn hình Đăng nhập (nút Back bị khóa không thể quay lại màn hình Quản trị).
6. *(Giai đoạn sau)* Quản lý nhân viên, tính lương và xem báo cáo tổng hợp.

### 10.2. Luồng Nhân viên (EMPLOYEE)

1. Mở ứng dụng và nhập tên đăng nhập `nv001` kèm mật khẩu `NhanVien@2026!`, bấm nút **Đăng nhập**.
2. Hệ thống xác thực thông tin, đối chiếu với Room, tự động tìm thấy mã nhân viên (`employeeId`) liên kết và điều hướng tới màn hình Nhân viên (`EmployeePlaceholderActivity`).
3. Màn hình hiển thị đầy đủ tên tài khoản, vai trò và mã nhân viên liên kết.
4. Tài khoản nhân viên bị chặn hoàn toàn khi cố tình truy cập vào các phương thức yêu cầu vai trò `ADMIN`.
5. Bấm nút **Đăng xuất**: phiên bị xóa và điều hướng an toàn về màn hình Đăng nhập.
6. *(Giai đoạn sau)* Thực hiện chấm công vào/ra và xem lương cá nhân.

## 11. Phân công 5 thành viên

| Thành viên | Nhóm công việc | Task chính | Trạng thái |
|---|---|---|---|
| **TV1** (Trưởng nhóm) | Nền tảng, đăng nhập, phiên, tích hợp | NEN-01, DN-01, DN-02, DN-03, DN-04 | **Hoàn thành** |
| TV2 | Quản lý nhân viên | NV-01, NV-02, NV-03, NV-04, NV-05 | Tiếp nhận API Auth |
| TV3 | Chấm công | CC-01, CC-02, CC-03, CC-04, CC-05 | Tiếp nhận Session |
| TV4 | Tính lương | LG-01, LG-02, LG-03, LG-04, LG-05 | Đang triển khai |
| TV5 | Báo cáo và kiểm thử | BC-01, BC-02, BC-03, KT-01, KT-02 | Đang triển khai |

### Hợp đồng API sẵn sàng cho TV2–TV5:

- **Dành cho TV2 (Tạo tài khoản nhân viên):**
  TV2 sử dụng phương thức `AuthService.hashPasswordForNewAccount(request.getInitialPassword())` và `AuthService.normalizeUsername(request.getUsername())` để tạo `Account` kèm `Employee` trong một Room transaction:
  ```java
  appDb.runInTransaction(() -> {
      String username = AuthService.normalizeUsername(request.getUsername());
      String passwordHash = AuthService.hashPasswordForNewAccount(request.getInitialPassword());
      Account newAccount = new Account(username, passwordHash, Constants.ROLE_EMPLOYEE);
      long accountId = appDb.accountDao().insert(newAccount);

      Employee newEmployee = new Employee(
              request.getCode().trim().toUpperCase(Locale.ROOT),
              request.getFullname().trim(),
              request.getEmail().trim().toLowerCase(Locale.ROOT),
              request.getPhone().trim(),
              request.getHourlyRate(),
              accountId
      );
      long employeeId = appDb.employeeDao().insert(newEmployee);
  });
  ```
- **Dành cho TV3 (Chấm công):**
  TV3 sử dụng `sessionManager.requireRole(Constants.ROLE_EMPLOYEE)` và `sessionManager.requireCurrentEmployeeId()` trên luồng nền để lấy ID nhân viên của phiên hiện tại một cách an toàn mà không nhận ID tùy ý từ giao diện.
- **Dành cho TV4 (Quản lý lương):**
  TV4 sử dụng `sessionManager.requireRole(Constants.ROLE_ADMIN)` để bảo vệ các chức năng tính lương và chốt trạng thái `PAID`.
- **Dành cho TV5 (Báo cáo cá nhân & Quản trị):**
  TV5 sử dụng `sessionManager.requireCurrentEmployeeId()` cho màn hình xem công/lương của chính nhân viên, và `sessionManager.requireRole(Constants.ROLE_ADMIN)` cho màn hình tổng quan toàn công ty.

## 12. Kiểm thử và kết quả thực tế

Các lệnh kiểm tra đã được thực thi thực tế trên môi trường phát triển:

| Lệnh kiểm thử | Kết quả thực tế |
|---|---|
| `JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug` | **BUILD SUCCESSFUL**, tạo thành công tệp APK `app-debug.apk` |
| `JAVA_HOME=/opt/android-studio/jbr ./gradlew testDebugUnitTest` | **BUILD SUCCESSFUL**, đạt **29/29 bài kiểm thử (100%)**, thời gian thực thi 1.1s |
| `JAVA_HOME=/opt/android-studio/jbr ./gradlew lintDebug` | **BUILD SUCCESSFUL**, **0 lỗi biên dịch lint** |

Danh sách các nhóm kiểm thử đã xác minh thành công:

1. `PasswordHasherTest` (6 bài kiểm thử):
   - Cùng một mật khẩu tạo ra hai chuỗi băm khác nhau do muối ngẫu nhiên (`salt`).
   - Mật khẩu chính xác được xác minh thành công.
   - Mật khẩu sai bị từ chối xác minh.
   - Chuỗi băm sai định dạng (thiếu token, sai tiền tố, số vòng âm, Base64 hỏng) không gây sập ứng dụng và được từ chối an toàn.
   - Chạy kiểm thử với mức 600.000 vòng lặp PBKDF2 theo khuyến nghị OWASP hoạt động chính xác.
   - Muối giải mã có độ dài tối thiểu 16 byte và khóa dẫn xuất đạt 256 bit.
2. `AuthServiceAndSessionManagerTest` (11 bài kiểm thử):
   - Chuẩn hóa tên đăng nhập (cắt khoảng trắng, chuyển chữ thường).
   - Đăng nhập Quản trị viên hợp lệ và lưu phiên.
   - Đăng nhập Nhân viên hợp lệ và lưu phiên.
   - Đăng nhập sai mật khẩu bị từ chối với thông báo tiếng Việt không làm lộ tài khoản.
   - Đăng nhập tài khoản không tồn tại bị từ chối với cùng thông báo an toàn.
   - Bỏ trống tên đăng nhập hoặc mật khẩu bị từ chối.
   - Thực thi kiểm tra vai trò: ngăn cản Nhân viên dùng quyền Admin và ngược lại.
   - Tự động hủy phiên SharedPreferences khi tài khoản bị xóa trong Room.
   - Tự động hủy phiên SharedPreferences khi vai trò tài khoản bị thay đổi trong Room.
   - Đăng xuất xóa sạch dữ liệu phiên lưu trữ.
   - Xác nhận không có mật khẩu thô hoặc chuỗi băm lưu trong SharedPreferences.
3. `DataInitializerUnitTest` (2 bài kiểm thử):
   - Khởi tạo thành công tài khoản Quản trị viên và Nhân viên mẫu với mật khẩu băm.
   - Đảm bảo tính idempotent: chạy nhiều lần không làm tăng số lượng tài khoản hoặc gây trùng lặp.
4. `SchemaContractTest` và `ConstantsTest` (10 bài kiểm thử):
   - Xác minh hợp đồng schema Room phiên bản 1 và các ràng buộc hằng số.

Phần chưa kiểm chứng:
- Bài kiểm thử `AuthInstrumentedTest` và `AppDatabaseTest` trong `src/androidTest/` đã được viết hoàn chỉnh nhưng chưa chạy trên thiết bị vật lý do môi trường hiện tại là máy chủ dòng lệnh headless không có KVM ảo hóa phần cứng cho emulator.

Không có bài kiểm thử nào bị bỏ qua, bỏ assert hoặc tắt để che giấu lỗi.

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
- Không có đăng ký tài khoản công khai; tài khoản Quản trị viên được khởi tạo từ hệ thống, tài khoản Nhân viên do Quản trị viên tạo.
- Không tính nghỉ trưa, ngày lễ, cuối tuần hoặc lương theo hợp đồng.
- Một nhân viên chỉ có tối đa một phiên chấm công mỗi ngày; không hỗ trợ ca qua nửa đêm.
- Màn hình `AdminPlaceholderActivity` và `EmployeePlaceholderActivity` là các màn hình điều phối tạm thời để kiểm thử phân quyền và đăng xuất trong khi chờ các màn hình chính thức từ TV2–TV5.
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
