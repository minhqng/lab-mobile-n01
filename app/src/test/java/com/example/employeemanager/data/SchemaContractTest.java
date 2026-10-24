package com.example.employeemanager.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Kiểm tra schema Room do annotation processor sinh ra so với đặc tả dữ liệu.
 *
 * <p>Bài kiểm thử đọc tệp {@code app/schemas/.../1.json}, vì vậy nó xác minh đúng
 * cấu trúc mà Room sẽ tạo trong SQLite thay vì chỉ kiểm tra mã nguồn Java.</p>
 */
public class SchemaContractTest {

    private static final String DATABASE_CLASS = "com.example.employeemanager.data.AppDB";

    private JSONObject database;

    @Before
    public void loadSchema() throws Exception {
        File schemaFile = new File("schemas/" + DATABASE_CLASS + "/1.json");
        if (!schemaFile.isFile()) {
            schemaFile = new File("app/schemas/" + DATABASE_CLASS + "/1.json");
        }
        assertTrue("Không tìm thấy tệp schema Room đã xuất", schemaFile.isFile());

        String content = new String(Files.readAllBytes(schemaFile.toPath()), StandardCharsets.UTF_8);
        database = new JSONObject(content).getJSONObject("database");
    }

    @Test
    public void databaseHasExactlyFourBusinessTables() throws Exception {
        assertEquals(1, database.getInt("version"));
        Set<String> tableNames = new HashSet<>();
        for (JSONObject entity : entities()) {
            tableNames.add(entity.getString("tableName"));
        }
        assertEquals(
                new HashSet<>(Arrays.asList("Account", "Employee", "WorkLog", "Salary")),
                tableNames);
    }

    @Test
    public void accountMatchesSpecification() throws Exception {
        JSONObject account = entity("Account");
        assertColumns(account, "id", "username", "password", "role");
        assertEquals("INTEGER", affinity(account, "id"));
        assertEquals("TEXT", affinity(account, "username"));
        assertEquals("TEXT", affinity(account, "password"));
        assertEquals("TEXT", affinity(account, "role"));
        assertTrue("id phải tự tăng", account.getJSONObject("primaryKey").getBoolean("autoGenerate"));
        assertTrue("username phải là duy nhất", hasUniqueIndex(account, "username"));
        assertTrue("Mọi cột Account đều bắt buộc", allColumnsNotNull(account));
    }

    @Test
    public void employeeMatchesSpecification() throws Exception {
        JSONObject employee = entity("Employee");
        assertColumns(employee, "id", "code", "fullname", "email", "phone", "hourlyRate", "accountId");
        assertEquals("INTEGER", affinity(employee, "hourlyRate"));
        assertEquals("INTEGER", affinity(employee, "accountId"));
        assertTrue("code phải là duy nhất", hasUniqueIndex(employee, "code"));
        assertTrue("email phải là duy nhất", hasUniqueIndex(employee, "email"));
        assertTrue("accountId phải là duy nhất", hasUniqueIndex(employee, "accountId"));
        assertForeignKey(employee, "accountId", "Account", "id", "RESTRICT");
        assertTrue("Mọi cột Employee đều bắt buộc", allColumnsNotNull(employee));
    }

    @Test
    public void workLogMatchesSpecification() throws Exception {
        JSONObject workLog = entity("WorkLog");
        assertColumns(workLog, "id", "employeeId", "workDate", "checkInTime", "checkOutTime", "totalHours", "status");
        assertEquals("TEXT", affinity(workLog, "workDate"));
        assertEquals("TEXT", affinity(workLog, "checkInTime"));
        assertEquals("TEXT", affinity(workLog, "checkOutTime"));
        assertEquals("REAL", affinity(workLog, "totalHours"));
        assertTrue(
                "Mỗi nhân viên chỉ có một bản ghi mỗi ngày",
                hasUniqueIndex(workLog, "employeeId", "workDate"));
        assertForeignKey(workLog, "employeeId", "Employee", "id", "RESTRICT");
        assertFalse("checkOutTime phải cho phép null",
                field(workLog, "checkOutTime").optBoolean("notNull", false));
        assertTrue("checkInTime phải bắt buộc", field(workLog, "checkInTime").getBoolean("notNull"));
    }

    @Test
    public void salaryMatchesSpecification() throws Exception {
        JSONObject salary = entity("Salary");
        assertColumns(salary, "id", "employeeId", "month", "year", "totalHours", "overtimeHours", "totalSalary", "status");
        assertEquals("INTEGER", affinity(salary, "month"));
        assertEquals("INTEGER", affinity(salary, "year"));
        assertEquals("REAL", affinity(salary, "totalHours"));
        assertEquals("REAL", affinity(salary, "overtimeHours"));
        assertEquals("INTEGER", affinity(salary, "totalSalary"));
        assertTrue(
                "Mỗi nhân viên chỉ có một bảng lương mỗi tháng/năm",
                hasUniqueIndex(salary, "employeeId", "month", "year"));
        assertForeignKey(salary, "employeeId", "Employee", "id", "RESTRICT");
        assertTrue("Mọi cột Salary đều bắt buộc", allColumnsNotNull(salary));
    }

    private List<JSONObject> entities() throws Exception {
        JSONArray array = database.getJSONArray("entities");
        List<JSONObject> result = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            result.add(array.getJSONObject(i));
        }
        return result;
    }

    private JSONObject entity(String tableName) throws Exception {
        for (JSONObject entity : entities()) {
            if (tableName.equals(entity.getString("tableName"))) {
                return entity;
            }
        }
        throw new AssertionError("Không tìm thấy bảng " + tableName + " trong schema Room");
    }

    private void assertColumns(JSONObject entity, String... expectedColumns) throws Exception {
        Set<String> actual = new HashSet<>();
        for (JSONObject field : fields(entity)) {
            actual.add(field.getString("columnName"));
        }
        assertEquals(
                "Danh sách cột của bảng " + entity.getString("tableName") + " không khớp đặc tả",
                new HashSet<>(Arrays.asList(expectedColumns)),
                actual);
    }

    private List<JSONObject> fields(JSONObject entity) throws Exception {
        JSONArray array = entity.getJSONArray("fields");
        List<JSONObject> result = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            result.add(array.getJSONObject(i));
        }
        return result;
    }

    private JSONObject field(JSONObject entity, String columnName) throws Exception {
        for (JSONObject field : fields(entity)) {
            if (columnName.equals(field.getString("columnName"))) {
                return field;
            }
        }
        throw new AssertionError("Không tìm thấy cột " + columnName);
    }

    private String affinity(JSONObject entity, String columnName) throws Exception {
        return field(entity, columnName).getString("affinity");
    }

    private boolean allColumnsNotNull(JSONObject entity) throws Exception {
        for (JSONObject field : fields(entity)) {
            if (!field.optBoolean("notNull", false)) {
                return false;
            }
        }
        return true;
    }

    private boolean hasUniqueIndex(JSONObject entity, String... columnNames) throws Exception {
        JSONArray indices = entity.optJSONArray("indices");
        if (indices == null) {
            return false;
        }
        Set<String> expected = new HashSet<>(Arrays.asList(columnNames));
        for (int i = 0; i < indices.length(); i++) {
            JSONObject index = indices.getJSONObject(i);
            if (!index.getBoolean("unique")) {
                continue;
            }
            JSONArray columns = index.getJSONArray("columnNames");
            Set<String> actual = new HashSet<>();
            for (int j = 0; j < columns.length(); j++) {
                actual.add(columns.getString(j));
            }
            if (expected.equals(actual)) {
                return true;
            }
        }
        return false;
    }

    private void assertForeignKey(JSONObject entity,
                                  String childColumn,
                                  String parentTable,
                                  String parentColumn,
                                  String onDelete) throws Exception {
        JSONArray foreignKeys = entity.optJSONArray("foreignKeys");
        assertNotNull("Bảng " + entity.getString("tableName") + " phải có khóa ngoại", foreignKeys);

        Map<String, String> expected = new HashMap<>();
        expected.put(childColumn, parentColumn);

        for (int i = 0; i < foreignKeys.length(); i++) {
            JSONObject foreignKey = foreignKeys.getJSONObject(i);
            if (!parentTable.equals(foreignKey.getString("table"))) {
                continue;
            }
            JSONArray columns = foreignKey.getJSONArray("columns");
            JSONArray referencedColumns = foreignKey.getJSONArray("referencedColumns");
            if (columns.length() == 1
                    && referencedColumns.length() == 1
                    && childColumn.equals(columns.getString(0))
                    && parentColumn.equals(referencedColumns.getString(0))) {
                assertEquals(onDelete, foreignKey.getString("onDelete"));
                return;
            }
        }
        throw new AssertionError(
                "Không tìm thấy khóa ngoại " + childColumn + " → " + parentTable + "." + parentColumn);
    }
}
