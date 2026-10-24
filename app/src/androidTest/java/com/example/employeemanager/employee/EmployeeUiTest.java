package com.example.employeemanager.employee;

import static org.junit.Assert.*;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.SystemClock;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.employeemanager.R;
import com.example.employeemanager.data.AppDB;
import com.example.employeemanager.data.entities.Account;
import com.example.employeemanager.data.entities.Employee;
import com.example.employeemanager.util.Constants;
import org.junit.*;
import org.junit.runner.RunWith;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.io.File;
import java.io.FileOutputStream;

/** Kiểm tra màn hình với Auth giả chỉ trong APK kiểm thử, không thay luồng đăng nhập thật. */
@RunWith(AndroidJUnit4.class)
public class EmployeeUiTest {
    private Instrumentation instrumentation;
    private AppDB database;
    private EmployeeService service;
    private long adminId;
    private volatile long sessionId;
    private final List<Activity> activities = new ArrayList<>();
    private EmployeeAuth auth;
    private CountDownLatch hashEntered, hashRelease;

    @Before public void setup() {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        database = AppDB.getInstance(instrumentation.getTargetContext());
        adminId = database.accountDao().insert(new Account("tv2-ui-admin", "bam-chi-dung-kiem-thu", Constants.ROLE_ADMIN));
        sessionId = adminId;
        auth = new EmployeeAuth() {
            @Override public long currentAccountId() { return sessionId; }
            @Override public String hashPassword(String plainPassword) {
                if (hashEntered != null) {
                    hashEntered.countDown();
                    try {
                        if (!hashRelease.await(8, TimeUnit.SECONDS)) throw new IllegalStateException();
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(exception);
                    }
                }
                return "bam-gia-chi-trong-apk-kiem-thu";
            }
        };
        EmployeeModule.configure(auth);
        service = new EmployeeService(database, auth);
    }

    @After public void cleanup() {
        if (hashRelease != null) hashRelease.countDown();
        instrumentation.runOnMainSync(() -> {
            for (Activity activity : activities) if (!activity.isFinishing()) activity.finish();
        });
        instrumentation.waitForIdleSync();
        sessionId = adminId;
        for (Employee employee : database.employeeDao().getAll()) {
            if (employee.code.startsWith("TV2-UI-")) service.deleteEmployee(employee.id);
        }
        Account admin = database.accountDao().findById(adminId);
        if (admin != null) database.accountDao().delete(admin);
        sessionId = 0;
        EmployeeModule.configure(new EmployeeAuth() {
            @Override public long currentAccountId() { return 0; }
            @Override public String hashPassword(String plainPassword) { throw new EmployeeException(null, R.string.nv_auth_chua_san_sang); }
        });
    }

    private Employee create() {
        return service.createEmployee(new CreateEmployeeRequest("TV2-UI-001", "Nguyễn Ánh",
                "tv2-ui@example.com", "0912345678", "50000", "tv2-ui-user", "đầu vào kiểm thử"));
    }
    private Activity launch(Class<? extends Activity> type, Long employeeId) {
        Intent intent = new Intent(instrumentation.getTargetContext(), type).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (employeeId != null) intent.putExtra(EmployeeActivity.EXTRA_EMPLOYEE_ID, employeeId);
        Activity activity = instrumentation.startActivitySync(intent);
        activities.add(activity);
        return activity;
    }
    private void awaitUi(BooleanSupplier condition) {
        long deadline = SystemClock.uptimeMillis() + 8000;
        AtomicBoolean ready = new AtomicBoolean();
        do {
            instrumentation.runOnMainSync(() -> ready.set(condition.getAsBoolean()));
            if (ready.get()) return;
            SystemClock.sleep(30);
        } while (SystemClock.uptimeMillis() < deadline);
        fail("Giao diện không đạt trạng thái yêu cầu trong thời gian chờ");
    }
    private void click(Activity activity, int id) {
        instrumentation.runOnMainSync(() -> activity.findViewById(id).performClick());
    }
    private void input(Activity activity, int id, String text) {
        instrumentation.runOnMainSync(() -> ((EditText) activity.findViewById(id)).setText(text));
    }
    private boolean statusIs(Activity activity, int resource) {
        return ((TextView) activity.findViewById(R.id.nv_status)).getText()
                .toString().equals(activity.getString(resource));
    }

    private AlertDialog deleteDialog(Activity activity) {
        try {
            java.lang.reflect.Field field = EmployeeDetailActivity.class.getDeclaredField("deleteDialog");
            field.setAccessible(true);
            return (AlertDialog) field.get(activity);
        } catch (ReflectiveOperationException exception) { throw new AssertionError(exception); }
    }

    private void fillCreateForm(Activity form) {
        input(form, R.id.nv_code, "TV2-UI-NEW");
        input(form, R.id.nv_fullname, "Trần Bình");
        input(form, R.id.nv_email, "tv2-ui-new@example.com");
        input(form, R.id.nv_phone, "0987654321");
        input(form, R.id.nv_hourly_rate, "50000");
        input(form, R.id.nv_username, "tv2-ui-new");
        input(form, R.id.nv_password, "đầu vào kiểm thử");
        input(form, R.id.nv_confirmation, "đầu vào kiểm thử");
    }

    private void capture(Activity activity, String name) {
        instrumentation.runOnMainSync(() -> {
            android.view.View view = activity.getWindow().getDecorView();
            Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
            view.draw(new Canvas(bitmap));
            File file = new File(activity.getFilesDir(), name + ".png");
            try (FileOutputStream stream = new FileOutputStream(file)) {
                assertTrue("Ảnh màn hình phải được ghi", bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream));
            } catch (java.io.IOException exception) { throw new AssertionError(exception); }
            finally { bitmap.recycle(); }
        });
    }

    @Test public void protectedScreensRejectMissingSessionAndEmployeeRole() {
        sessionId = 0;
        awaitUi(launch(EmployeeListActivity.class, null)::isFinishing);
        awaitUi(launch(EmployeeFormActivity.class, null)::isFinishing);
        awaitUi(launch(EmployeeDetailActivity.class, 999L)::isFinishing);
        sessionId = adminId;
        Employee employee = create();
        sessionId = employee.accountId;
        awaitUi(launch(EmployeeListActivity.class, null)::isFinishing);
    }

    @Test public void listSearchAndVietnameseCurrencyRenderCorrectly() {
        create();
        Activity list = launch(EmployeeListActivity.class, null);
        awaitUi(() -> statusIs(list, R.string.nv_danh_sach_da_tai));
        input(list, R.id.nv_keyword, "NGUYỄN ÁNH");
        awaitUi(() -> statusIs(list, R.string.nv_danh_sach_da_tai)
                && ((RecyclerView) list.findViewById(R.id.nv_list)).getAdapter().getItemCount() == 1);
        awaitUi(() -> {
            RecyclerView recycler = list.findViewById(R.id.nv_list);
            RecyclerView.ViewHolder holder = recycler.findViewHolderForAdapterPosition(0);
            return holder != null && ((TextView) holder.itemView.findViewById(R.id.nv_hourly_rate))
                    .getText().toString().equals("50.000 đ/giờ");
        });
        capture(list, "nv-danh-sach");
        input(list, R.id.nv_keyword, "%");
        awaitUi(() -> statusIs(list, R.string.nv_khong_tim_thay));
        input(list, R.id.nv_keyword, "");
        awaitUi(() -> statusIs(list, R.string.nv_danh_sach_da_tai));
    }

    @Test public void createFormShowsFieldErrorThenSavesRealPair() {
        Activity form = launch(EmployeeFormActivity.class, null);
        awaitUi(() -> statusIs(form, R.string.nv_huong_dan_form));
        click(form, R.id.nv_save);
        awaitUi(() -> ((EditText) form.findViewById(R.id.nv_code)).getError() != null);
        capture(form, "nv-bieu-mau");
        fillCreateForm(form);
        input(form, R.id.nv_confirmation, "không khớp");
        click(form, R.id.nv_save);
        awaitUi(() -> ((EditText) form.findViewById(R.id.nv_confirmation)).getError() != null);
        input(form, R.id.nv_confirmation, "đầu vào kiểm thử");
        click(form, R.id.nv_save);
        awaitUi(form::isFinishing);
        Employee saved = database.employeeDao().findByCode("TV2-UI-NEW");
        assertNotNull(saved);
        assertEquals("tv2-ui-new", database.accountDao().findById(saved.accountId).username);
    }

    @Test public void editSavesAndListRefreshesOnReturn() {
        Employee employee = create();
        Activity list = launch(EmployeeListActivity.class, null);
        awaitUi(() -> statusIs(list, R.string.nv_danh_sach_da_tai));
        Activity form = launch(EmployeeFormActivity.class, employee.id);
        awaitUi(() -> ((EditText) form.findViewById(R.id.nv_code)).getText().toString().equals(employee.code));
        input(form, R.id.nv_fullname, "Trần Bình");
        input(form, R.id.nv_hourly_rate, "75000");
        click(form, R.id.nv_save);
        awaitUi(form::isFinishing);
        awaitUi(() -> statusIs(list, R.string.nv_danh_sach_da_tai)
                && list.findViewById(R.id.nv_root).isShown());
        assertEquals("Trần Bình", database.employeeDao().findById(employee.id).fullname);
        assertEquals(employee.accountId, database.employeeDao().findById(employee.id).accountId);
    }

    @Test public void emptyAndMissingEmployeeStatesAreHandled() {
        Activity list = launch(EmployeeListActivity.class, null);
        awaitUi(() -> statusIs(list, R.string.nv_chua_co));
        Activity detail = launch(EmployeeDetailActivity.class, 999999L);
        awaitUi(() -> statusIs(detail, R.string.nv_khong_ton_tai));
        instrumentation.runOnMainSync(() -> {
            assertFalse(detail.findViewById(R.id.nv_delete).isEnabled());
            detail.findViewById(R.id.nv_back).performClick();
        });
        awaitUi(detail::isFinishing);
    }

    @Test public void deleteRequiresConfirmationAndRefreshesList() {
        Employee employee = create();
        Activity list = launch(EmployeeListActivity.class, null);
        awaitUi(() -> statusIs(list, R.string.nv_danh_sach_da_tai));
        Activity detail = launch(EmployeeDetailActivity.class, employee.id);
        awaitUi(() -> ((TextView) detail.findViewById(R.id.nv_code)).getText().toString().equals(employee.code));
        capture(detail, "nv-chi-tiet");
        click(detail, R.id.nv_delete);
        instrumentation.waitForIdleSync();
        instrumentation.runOnMainSync(() -> {
            assertTrue(deleteDialog(detail).isShowing());
            deleteDialog(detail).getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
        });
        assertNotNull(database.employeeDao().findById(employee.id));
        click(detail, R.id.nv_delete);
        instrumentation.waitForIdleSync();
        instrumentation.runOnMainSync(() -> deleteDialog(detail)
                .getButton(AlertDialog.BUTTON_POSITIVE).performClick());
        // Kết quả xác nhận được kiểm tra ở dữ liệu; không suy đoán từ Toast.
        awaitUi(detail::isFinishing);
        assertNull(database.employeeDao().findById(employee.id));
        assertNull(database.accountDao().findById(employee.accountId));
        awaitUi(() -> statusIs(list, R.string.nv_chua_co));
    }

    @Test public void rotationDuringSaveKeepsOneMutationAndDeliversResult() throws Exception {
        hashEntered = new CountDownLatch(1);
        hashRelease = new CountDownLatch(1);
        Activity form = launch(EmployeeFormActivity.class, null);
        awaitUi(() -> statusIs(form, R.string.nv_huong_dan_form));
        fillCreateForm(form);
        click(form, R.id.nv_save);
        assertTrue("Phép băm phải bắt đầu trên luồng nền", hashEntered.await(5, TimeUnit.SECONDS));
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(EmployeeFormActivity.class.getName(), null, false);
        Activity recreated;
        try {
            instrumentation.runOnMainSync(form::recreate);
            recreated = monitor.waitForActivityWithTimeout(8000);
        } finally { instrumentation.removeMonitor(monitor); }
        assertNotNull("Biểu mẫu phải được tái tạo", recreated);
        activities.add(recreated);
        awaitUi(() -> recreated.findViewById(R.id.nv_root).isShown());
        instrumentation.runOnMainSync(() -> assertFalse(recreated.findViewById(R.id.nv_save).isEnabled()));
        hashRelease.countDown();
        awaitUi(recreated::isFinishing);
        assertNotNull(database.employeeDao().findByCode("TV2-UI-NEW"));
        assertEquals(1, database.employeeDao().search("TV2-UI-NEW").size());
    }

    @Test public void duplicateSaveKeepsFormAndShowsFieldError() {
        create();
        Activity form = launch(EmployeeFormActivity.class, null);
        awaitUi(() -> statusIs(form, R.string.nv_huong_dan_form));
        fillCreateForm(form);
        input(form, R.id.nv_code, "TV2-UI-001");
        click(form, R.id.nv_save);
        awaitUi(() -> statusIs(form, R.string.nv_trung_ma));
        instrumentation.runOnMainSync(() -> {
            assertFalse(form.isFinishing());
            assertEquals("Trần Bình", ((EditText) form.findViewById(R.id.nv_fullname)).getText().toString());
            assertNotNull(((EditText) form.findViewById(R.id.nv_code)).getError());
            assertTrue(form.findViewById(R.id.nv_save).isEnabled());
        });
        assertNull(database.accountDao().findByUsername("tv2-ui-new"));
    }
}
