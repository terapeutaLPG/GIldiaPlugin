package pl.gildia.utils;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Utility do konwersji UUID na BINARY(16) dla MySQL
 */
public class UUIDUtil {

    /**
     * Konwertuje UUID na byte array dla BINARY(16)
     */
    public static byte[] uuidToBytes(UUID uuid) {
        if (uuid == null) {
            return null;
        }

        ByteBuffer bb = ByteBuffer.allocate(16);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    /**
     * Konwertuje byte array z BINARY(16) na UUID
     */
    public static UUID bytesToUuid(byte[] bytes) {
        if (bytes == null || bytes.length != 16) {
            return null;
        }

        ByteBuffer bb = ByteBuffer.wrap(bytes);
        long mostSigBits = bb.getLong();
        long leastSigBits = bb.getLong();
        return new UUID(mostSigBits, leastSigBits);
    }

    /**
     * Konwertuje UUID na String bez myślników dla kompatybilności
     */
    public static String uuidToString(UUID uuid) {
        return uuid != null ? uuid.toString().replace("-", "") : null;
    }

    /**
     * Konwertuje String bez myślników na UUID
     */
    public static UUID stringToUuid(String str) {
        if (str == null || str.length() != 32) {
            return null;
        }

        String formatted = str.substring(0, 8) + "-"
                + str.substring(8, 12) + "-"
                + str.substring(12, 16) + "-"
                + str.substring(16, 20) + "-"
                + str.substring(20, 32);

        try {
            return UUID.fromString(formatted);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
