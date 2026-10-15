package com.securetravels.crm.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Production secret hygiene. Logs loudly when operators still need to set
 * Railway Variables; does not crash the process (crash-loops hide the real
 * database / networking errors on first deploy).
 */
@Component
@Profile("prod")
public class ProdSecretsValidator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ProdSecretsValidator.class);

    private static final String DEV_JWT = "securetravels-phase1-jwt-secret-change-me-32b-plus";
    private static final String DEV_WEBHOOK = "dev-webhook-secret-insecure-change-me";

    private final Environment env;

    public ProdSecretsValidator(Environment env) {
        this.env = env;
    }

    @Override
    public void run(ApplicationArguments args) {
        String jwt = firstNonBlank(env.getProperty("JWT_SECRET"), env.getProperty("app.jwt.secret"), "");
        String webhook = firstNonBlank(env.getProperty("WEBHOOK_SECRET"), env.getProperty("app.webhook.secret"), "");

        if (jwt.isBlank() || jwt.equals(DEV_JWT) || jwt.length() < 32) {
            log.error(
                    "[prod-secrets] Set JWT_SECRET in Railway Variables (openssl rand -base64 48). "
                            + "Without it, logins reset on every redeploy.");
        }
        if (webhook.isBlank() || webhook.equals(DEV_WEBHOOK)) {
            log.warn(
                    "[prod-secrets] Set WEBHOOK_SECRET in Railway Variables before enabling webhooks");
        } else if (jwt.length() >= 32 && !jwt.equals(DEV_JWT)) {
            log.info("[prod-secrets] JWT_SECRET and WEBHOOK_SECRET look configured");
        }

        // Meta: presence only — never log values. Detailed summary is MetaConfigReporter.
        boolean metaToken = present(env.getProperty("META_ACCESS_TOKEN"))
                || present(env.getProperty("app.meta.access-token"));
        if (!metaToken) {
            log.warn(
                    "[prod-secrets] META_ACCESS_TOKEN unset — Meta Ads/Campaigns APIs unavailable until configured");
        }
    }

    private static boolean present(String value) {
        return value != null && !value.isBlank();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
