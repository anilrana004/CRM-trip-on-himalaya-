package com.securetravels.crm.marketing;

import com.securetravels.crm.common.security.HmacSigner;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class MetaHubSignatureTest {

    @Test
    void acceptsValidSha256Header() {
        byte[] body = "{\"object\":\"page\"}".getBytes(StandardCharsets.UTF_8);
        String secret = "app-secret";
        String header = HmacSigner.sign(secret, body);
        assertThat(MetaHubSignature.matches(header, secret, body)).isTrue();
    }

    @Test
    void rejectsWrongSecret() {
        byte[] body = "{\"object\":\"page\"}".getBytes(StandardCharsets.UTF_8);
        String header = HmacSigner.sign("app-secret", body);
        assertThat(MetaHubSignature.matches(header, "other-secret", body)).isFalse();
    }

    @Test
    void rejectsBlankAppSecret() {
        byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
        assertThat(MetaHubSignature.matches("sha256=abc", "", body)).isFalse();
    }
}
