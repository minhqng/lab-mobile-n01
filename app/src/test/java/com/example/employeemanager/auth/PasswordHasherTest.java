package com.example.employeemanager.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Base64;

/**
 * Kiểm thử đơn vị cho tiện ích băm và xác minh mật khẩu {@link PasswordHasher}.
 */
public class PasswordHasherTest {

    @Test
    public void testHashFormatAndPrefix() {
        String password = "MatKhauBaoMat@2026!";
        // Dùng 10.000 vòng để kiểm tra định dạng nhanh
        String hash = PasswordHasher.hashPassword(password, 10_000);

        assertNotNull(hash);
        String[] parts = hash.split("\\$");
        assertEquals("Chuỗi băm phải có 4 phần phân cách bởi dấu $", 4, parts.length);
        assertEquals("Tiền tố thuật toán phải là pbkdf2_sha256", PasswordHasher.ALGORITHM_PREFIX, parts[0]);
        assertEquals("Số vòng phải khớp với tham số", "10000", parts[1]);

        byte[] salt = Base64.getDecoder().decode(parts[2]);
        assertTrue("Độ dài salt phải tối thiểu 16 byte", salt.length >= PasswordHasher.SALT_LENGTH_BYTES);

        byte[] derivedKey = Base64.getDecoder().decode(parts[3]);
        assertEquals("Độ dài khóa dẫn xuất phải là 32 byte (256 bit)", 32, derivedKey.length);
    }

    @Test
    public void testSamePasswordGeneratesDifferentHashesDueToSalt() {
        String password = "QuanLyChung@2026!";
        String hash1 = PasswordHasher.hashPassword(password, 2_000);
        String hash2 = PasswordHasher.hashPassword(password, 2_000);

        assertNotEquals("Cùng một mật khẩu phải tạo ra chuỗi hash khác nhau do muối ngẫu nhiên", hash1, hash2);

        String salt1 = hash1.split("\\$")[2];
        String salt2 = hash2.split("\\$")[2];
        assertNotEquals("Muối ngẫu nhiên của hai lần băm phải khác nhau", salt1, salt2);

        assertTrue("Hash 1 phải xác minh thành công với mật khẩu gốc", PasswordHasher.verifyPassword(password, hash1));
        assertTrue("Hash 2 phải xác minh thành công với mật khẩu gốc", PasswordHasher.verifyPassword(password, hash2));
    }

    @Test
    public void testCorrectPasswordVerifiesSuccessfully() {
        String password = "MatKhauChinhXac#123";
        String hash = PasswordHasher.hashPassword(password, 5_000);

        assertTrue("Mật khẩu chính xác phải được xác minh thành công", PasswordHasher.verifyPassword(password, hash));
    }

    @Test
    public void testWrongPasswordRejected() {
        String correctPassword = "MatKhauDung#123";
        String wrongPassword = "MatKhauSai#123";
        String hash = PasswordHasher.hashPassword(correctPassword, 5_000);

        assertFalse("Mật khẩu sai phải bị từ chối xác minh", PasswordHasher.verifyPassword(wrongPassword, hash));
    }

    @Test
    public void testInvalidHashFormatsDoNotCrash() {
        String password = "BatKyMatKhauNao";

        // Null hoặc rỗng
        assertFalse("Hash null không được gây crash", PasswordHasher.verifyPassword(password, null));
        assertFalse("Hash rỗng không được gây crash", PasswordHasher.verifyPassword(password, ""));
        assertFalse("Mật khẩu null không được gây crash", PasswordHasher.verifyPassword(null, "pbkdf2_sha256$1000$c2FsdA==$aGFzaA=="));
        assertFalse("Mật khẩu rỗng không được gây crash", PasswordHasher.verifyPassword("", "pbkdf2_sha256$1000$c2FsdA==$aGFzaA=="));

        // Thiếu phần
        assertFalse("Hash thiếu trường không được gây crash", PasswordHasher.verifyPassword(password, "pbkdf2_sha256$1000$c2FsdA=="));
        assertFalse("Hash chuỗi ngẫu nhiên không được gây crash", PasswordHasher.verifyPassword(password, "chuoi_khong_hop_le_hoan_toan"));

        // Sai thuật toán
        assertFalse("Sai thuật toán phải bị từ chối", PasswordHasher.verifyPassword(password, "md5$1000$c2FsdA==$aGFzaA=="));

        // Số vòng không phải số hoặc <= 0
        assertFalse("Số vòng không hợp lệ phải bị từ chối", PasswordHasher.verifyPassword(password, "pbkdf2_sha256$abc$c2FsdA==$aGFzaA=="));
        assertFalse("Số vòng âm phải bị từ chối", PasswordHasher.verifyPassword(password, "pbkdf2_sha256$-100$c2FsdA==$aGFzaA=="));

        // Base64 hỏng
        assertFalse("Base64 salt hỏng không được gây crash", PasswordHasher.verifyPassword(password, "pbkdf2_sha256$1000$!!!khong_phai_base64$$$"));
    }

    @Test
    public void testDefaultIterationsExecution() {
        // Kiểm tra thực tế với 600.000 vòng theo đặc tả để đảm bảo chạy đúng
        String demoPassword = "QuanLy@2026!";
        String hash = PasswordHasher.hashPassword(demoPassword);

        assertNotNull(hash);
        assertTrue("Chuỗi băm mặc định phải chứa 600000 vòng", hash.contains("$600000$"));
        assertTrue("Mật khẩu demo phải xác minh thành công với 600.000 vòng", PasswordHasher.verifyPassword(demoPassword, hash));
        assertFalse("Mật khẩu sai phải thất bại với 600.000 vòng", PasswordHasher.verifyPassword("QuanLy@2025!", hash));
    }
}
