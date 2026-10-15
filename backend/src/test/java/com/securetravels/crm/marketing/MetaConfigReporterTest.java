package com.securetravels.crm.marketing;

import com.securetravels.crm.common.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import static org.assertj.core.api.Assertions.assertThatCode;

class MetaConfigReporterTest {

    @Test
    void runsWithoutThrowingWhenMetaUnset() {
        AppProperties props = new AppProperties();
        MetaConfigReporter reporter = new MetaConfigReporter(props);
        assertThatCode(() -> reporter.run(new DefaultApplicationArguments(new String[0])))
                .doesNotThrowAnyException();
    }

    @Test
    void runsWithoutThrowingWhenMetaPresent() {
        AppProperties props = new AppProperties();
        props.getMeta().setAccessToken("not-a-real-token");
        props.getMeta().setAppSecret("not-a-real-secret");
        props.getMeta().setWebhookVerifyToken("verify");
        props.getMeta().setAppId("123");
        props.getMeta().setAdAccountIds("act_1");
        props.getMeta().setGraphVersion("v22.0");

        MetaConfigReporter reporter = new MetaConfigReporter(props);
        assertThatCode(() -> reporter.run(new DefaultApplicationArguments(new String[0])))
                .doesNotThrowAnyException();
    }

    @Test
    void flagHelperIsNonSensitive() {
        assertThatCode(() -> {
            if (!"set".equals(MetaConfigReporter.flag(true))) {
                throw new IllegalStateException("expected set");
            }
            if (!"missing".equals(MetaConfigReporter.flag(false))) {
                throw new IllegalStateException("expected missing");
            }
        }).doesNotThrowAnyException();
    }
}
