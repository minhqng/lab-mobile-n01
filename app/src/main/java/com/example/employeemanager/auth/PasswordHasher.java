package com.example.employeemanager.auth;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Tiện ích băm và xác minh mật khẩu bằng thuật toán PBKDF2WithHmacSHA256.
 *
 * <p>Tuân thủ khuyến nghị bảo mật OWASP với 600.000 vòng băm, muối ngẫu nhiên (salt)
 * 16 byte và khóa dẫn xuất 256 bit. Chuỗi kết quả lưu trữ bao gồm định danh thuật toán,
 * số vòng băm, muối và giá trị băm được mã hóa Base64 theo định dạng:
 * {@code pbkdf2_sha256$<iterations>$<saltBase64>$<hashBase64>}.</p>
 *
 * <p>Tác vụ băm mật khẩu đòi hỏi chi phí tính toán cao, do đó phải luôn được thực thi
 * trên luồng nền (ví dụ thông qua {@code AppExecutors.databaseExecutor()}), tuyệt đối
 * không chạy trực tiếp trên luồng giao diện (UI thread).</p>
 */
public final class PasswordHasher {

    /** Định danh thuật toán lưu trữ trong chuỗi hash. */
    public static final String ALGORITHM_PREFIX = "pbkdf2_sha256";

    /** Tên thuật toán mật mã chuẩn trong Java Cryptography Architecture. */
    private static final String SECRET_KEY_ALGORITHM = "PBKDF2WithHmacSHA256";

    /** Số vòng băm mặc định theo khuyến nghị OWASP cho PBKDF2-HMAC-SHA256. */
    public static final int DEFAULT_ITERATIONS = 600_000;

    /** Độ dài muối ngẫu nhiên (salt) tính theo byte (16 byte = 128 bit). */
    public static final int SALT_LENGTH_BYTES = 16;

    /** Độ dài khóa dẫn xuất tính theo bit (256 bit = 32 byte). */
    public static final int DERIVED_KEY_LENGTH_BITS = 256;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordHasher() {
        // Lớp tiện ích tĩnh, không được khởi tạo.
    }

    /**
     * Băm mật khẩu dạng rõ với số vòng băm mặc định (600.000 vòng) và muối ngẫu nhiên mới.
     *
     * @param plainPassword Mật khẩu thô dạng rõ cần băm.
     * @return Chuỗi định dạng chuẩn chứa thuật toán, số vòng, muối và giá trị băm.
     * @throws IllegalArgumentException nếu mật khẩu rỗng hoặc null.
     */
    public static String hashPassword(String plainPassword) {
        return hashPassword(plainPassword, DEFAULT_ITERATIONS);
    }

    /**
     * Băm mật khẩu dạng rõ với số vòng băm tùy chọn (hữu ích cho kiểm thử nhanh).
     *
     * @param plainPassword Mật khẩu thô dạng rõ cần băm.
     * @param iterations    Số vòng lặp băm PBKDF2 (phải lớn hơn 0).
     * @return Chuỗi định dạng chuẩn chứa thuật toán, số vòng, muối và giá trị băm.
     * @throws IllegalArgumentException nếu mật khẩu rỗng, null hoặc số vòng không hợp lệ.
     */
    public static String hashPassword(String plainPassword, int iterations) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Mật khẩu không được để trống.");
        }
        if (iterations <= 0) {
            throw new IllegalArgumentException("Số vòng băm phải lớn hơn 0.");
        }

        byte[] salt = new byte[SALT_LENGTH_BYTES];
        SECURE_RANDOM.nextBytes(salt);

        byte[] hash = computePbkdf2(plainPassword.toCharArray(), salt, iterations, DERIVED_KEY_LENGTH_BITS);

        Base64.Encoder encoder = Base64.getEncoder();
        String saltBase64 = encoder.encodeToString(salt);
        String hashBase64 = encoder.encodeToString(hash);

        return ALGORITHM_PREFIX + "$" + iterations + "$" + saltBase64 + "$" + hashBase64;
    }

    /**
     * Xác minh mật khẩu thô đối chiếu với chuỗi băm đã lưu trữ.
     *
     * <p>Phương thức sử dụng so sánh an toàn về mặt thời gian (constant-time comparison)
     * để ngăn chặn tấn công timing attack. Định dạng chuỗi lưu trữ không hợp lệ sẽ bị
     * từ chối an toàn bằng cách trả về {@code false} thay vì gây crash ứng dụng.</p>
     *
     * @param plainPassword Mật khẩu thô do người dùng nhập.
     * @param storedHash    Chuỗi băm lưu trong cơ sở dữ liệu.
     * @return {@code true} nếu mật khẩu khớp, {@code false} nếu sai hoặc chuỗi băm hỏng.
     */
    public static boolean verifyPassword(String plainPassword, String storedHash) {
        if (plainPassword == null || plainPassword.isEmpty() || storedHash == null || storedHash.isEmpty()) {
            return false;
        }

        try {
            String[] parts = storedHash.split("\\$");
            if (parts.length != 4) {
                return false;
            }

            String algorithm = parts[0];
            if (!ALGORITHM_PREFIX.equals(algorithm)) {
                return false;
            }

            int iterations = Integer.parseInt(parts[1]);
            if (iterations <= 0) {
                return false;
            }

            Base64.Decoder decoder = Base64.getDecoder();
            byte[] salt = decoder.decode(parts[2]);
            byte[] expectedHash = decoder.decode(parts[3]);

            if (salt.length < SALT_LENGTH_BYTES || expectedHash.length == 0) {
                return false;
            }

            byte[] actualHash = computePbkdf2(plainPassword.toCharArray(), salt, iterations, expectedHash.length * 8);

            return MessageDigest.isEqual(actualHash, expectedHash);
        } catch (Exception e) {
            // Không làm sập ứng dụng khi chuỗi hash sai định dạng, parse lỗi hoặc base64 hỏng.
            return false;
        }
    }

    /**
     * Thực hiện tính toán PBKDF2 bằng Java Cryptography Extension.
     */
    private static byte[] computePbkdf2(char[] passwordChars, byte[] salt, int iterations, int keyLengthBits) {
        try {
            KeySpec spec = new PBEKeySpec(passwordChars, salt, iterations, keyLengthBits);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(SECRET_KEY_ALGORITHM);
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Lỗi cấu hình mật mã hệ thống PBKDF2: " + e.getMessage(), e);
        }
    }
}
