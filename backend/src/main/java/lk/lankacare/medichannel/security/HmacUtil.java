package lk.lankacare.medichannel.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Generates deterministic HMAC-SHA256 blind indexes for searching and enforcing
 * uniqueness on encrypted fields (email and Sri Lankan NIC).
 */
@Component
public class HmacUtil {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private final SecretKeySpec keySpec;

    public HmacUtil(@Value("${app.encryption.hmac-secret}") String hmacSecret) {
        if (hmacSecret == null || hmacSecret.isBlank()) {
            throw new IllegalStateException("HMAC_SECRET environment variable must be set.");
        }
        this.keySpec = new SecretKeySpec(hmacSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
    }

    public String hashNormalized(String rawValue) {
        if (rawValue == null) {
            return null;
        }
        String normalized = rawValue.trim().toLowerCase();
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(keySpec);
            byte[] digest = mac.doFinal(normalized.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to compute HMAC-SHA256 blind index.", ex);
        }
    }
}
