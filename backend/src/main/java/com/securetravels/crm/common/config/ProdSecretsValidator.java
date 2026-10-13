package com.securetravels.crm.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Fail fast in production when critical secrets are still the checked-in
 * development defaults. Prefer a loud boot failure over a silently insecure
 * deployment.
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
        String jwt = env.getProperty("app.jwt.secret", "");
        String webhook = env.getProperty("app.webhook.secret", "");

        if (jwt.isBlank() || jwt.equals(DEV_JWT) || jwt.length() < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET must be set in production to a random value of at least 32 characters");
        }
        if (webhook.isBlank() || webhook.equals(DEV_WEBHOOK)) {
            throw new IllegalStateException(
                    "WEBHOOK_SECRET must be set in production (do not use the development default)");
        }
        log.info("[prod-secrets] JWT_SECRET and WEBHOOK_SECRET look configured");
    }
}
