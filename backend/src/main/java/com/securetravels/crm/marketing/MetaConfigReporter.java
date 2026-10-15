package com.securetravels.crm.marketing;

import com.securetravels.crm.common.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Boots with a non-sensitive Meta configuration summary (presence flags only).
 * Never logs token, app secret, or verify-token values. Does not fail startup —
 * Ads endpoints already fail closed at request time when the access token is
 * missing.
 */
@Component
@Order(50)
public class MetaConfigReporter implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MetaConfigReporter.class);

    private final AppProperties props;

    public MetaConfigReporter(AppProperties props) {
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        AppProperties.Meta meta = props.getMeta();
        boolean token = present(meta.getAccessToken());
        boolean appSecret = present(meta.getAppSecret());
        boolean verifyToken = present(meta.getWebhookVerifyToken());
        boolean appId = present(meta.getAppId());
        boolean allowlist = present(meta.getAdAccountIds());
        String version = blankToDefault(meta.getGraphVersion(), "v22.0");

        log.info(
                "[meta-config] accessToken={} appSecret={} webhookVerifyToken={} appId={} "
                        + "adAccountAllowlist={} graphVersion={} (values never logged)",
                flag(token),
                flag(appSecret),
                flag(verifyToken),
                flag(appId),
                flag(allowlist),
                version);

        if (!token) {
            log.warn(
                    "[meta-config] META_ACCESS_TOKEN is unset — "
                            + "GET /api/marketing/meta/* will return 503 until configured on the server");
        }
        if (!appSecret || !verifyToken) {
            log.warn(
                    "[meta-config] Lead Ads webhook incomplete — set META_APP_SECRET and "
                            + "META_WEBHOOK_VERIFY_TOKEN before Meta can verify/deliver to /api/webhook/meta");
        }
        if (!appId) {
            log.info(
                    "[meta-config] META_APP_ID unset (optional; reserved for ops/diagnostics, not required by Graph calls)");
        }
    }

    static String flag(boolean present) {
        return present ? "set" : "missing";
    }

    private static boolean present(String value) {
        return value != null && !value.isBlank();
    }

    private static String blankToDefault(String value, String fallback) {
        return present(value) ? value.trim() : fallback;
    }
}
