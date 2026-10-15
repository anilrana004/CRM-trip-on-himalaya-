package com.securetravels.crm.marketing;

import com.securetravels.crm.common.security.HmacSigner;

/**
 * Meta webhook signing: {@code X-Hub-Signature-256: sha256=<hex>} over the raw
 * body using the app secret (same primitive as website/Interakt HMAC).
 */
public final class MetaHubSignature {

    private MetaHubSignature() {}

    public static final String HEADER = "X-Hub-Signature-256";

    public static boolean matches(String provided, String appSecret, byte[] payload) {
        if (appSecret == null || appSecret.isBlank()) {
            return false;
        }
        return HmacSigner.matches(provided, appSecret, payload);
    }

    public static String sign(String appSecret, byte[] payload) {
        return HmacSigner.sign(appSecret, payload);
    }
}
