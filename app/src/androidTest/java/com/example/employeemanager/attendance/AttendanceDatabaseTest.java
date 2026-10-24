package com.example.employeemanager.attendance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;

import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.employeemanager.data.AppDB;
import com.example.employeemanager.data.entities.WorkLog;
import com.example.employeemanager.util.Constants;
import com.example.employeemanager.util.DateTimeUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

@RunWith(AndroidJUnit4.class)
public class AttendanceDatabaseTest {

    private AppDB database;

    @Before
    public void createDatabase() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDB.class).build();
        // Tạo dữ liệu nhân viên tối thiểu để kiểm thử chấm công.
        long accountId = database.accountDao().insert(
                new com.example.employeemanager.data.entities.Account(
                        "nvtest", "hash", Constants.ROLE_EMPLOYEE));
        database.employeeDao().insert(
                new com.example.employeemanager.data.entities.Employee(
                        "NVTEST", "Nhân viên thử nghiệm", "test@example.com",
                        "0912345678", 50000L, accountId));
    }

    @After
    public void closeDatabase() {
        database.close();
    }

    @Test
    public void checkInAndCheckOutPersistWorkLog() {
        long employeeId = database.employeeDao().findByAccountId(1L).id;
        Clock clock = Clock.fixed(Instant.parse("2026-10-01T01:00:00Z"),
                ZoneId.of(Constants.TIME_ZONE_ID));
        AttendanceService service = new AttendanceService(
                database.workLogDao(), database.salaryDao(), () -> employeeId, clock);

        WorkLog checkedIn = service.checkIn();
        assertNotNull(checkedIn);

        service.setClock(Clock.fixed(Instant.parse("2026-10-01T09:00:00Z"),
                ZoneId.of(Constants.TIME_ZONE_ID)));
        WorkLog checkedOut = service.checkOut();
        assertEquals(Constants.STATUS_COMPLETED, checkedOut.status);
        assertEquals(8.0, checkedOut.totalHours, 0.0001);

        assertEquals(1, database.workLogDao().findByEmployeeAndMonth(
                employeeId, "2026-10").size());
    }
}
