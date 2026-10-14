package groceryshop;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PinSecurity {

    private static final String PREFIX = "pbkdf2_sha256$";
    private static final int ITERATIONS = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PinSecurity() {
    }

    public static String hash(String pin) throws GeneralSecurityException {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] derived = derive(pin, salt, ITERATIONS, KEY_BITS);

        return PREFIX + ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(derived);
    }

    public static boolean isHashed(String value) {
        return value != null && value.startsWith(PREFIX);
    }

    public static boolean verify(String pin, String encoded)
            throws GeneralSecurityException {
        if (pin == null || !isHashed(encoded)) {
            return false;
        }

        String[] parts = encoded.split("\\$", -1);
        if (parts.length != 4 || !"pbkdf2_sha256".equals(parts[0])) {
            return false;
        }

        try {
            int iterations = Integer.parseInt(parts[1]);
            if (iterations < 1 || iterations > 2_000_000) {
                return false;
            }

            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            if (salt.length < 8 || expected.length == 0) {
                return false;
            }

            byte[] actual = derive(pin, salt, iterations, expected.length * 8);
            if (actual.length != expected.length) {
                return false;
            }

            int difference = 0;
            for (int i = 0; i < expected.length; i++) {
                difference |= actual[i] ^ expected[i];
            }
            return difference == 0;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] derive(String pin, byte[] salt, int iterations, int bits)
            throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(pin.toCharArray(), salt, iterations, bits);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }
}
