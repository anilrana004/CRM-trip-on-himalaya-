package com.securetravels.crm.marketing;

import com.securetravels.crm.marketing.dto.MetaApiCallLogResponse;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards against accidentally persisting secrets or lead PII in the Ads API
 * usage log used for Standard Access evidence.
 */
class MetaApiCallLogHygieneTest {

    private static final Set<String> FORBIDDEN_FRAGMENTS = Set.of(
            "token", "secret", "password", "email", "phone", "payload", "body", "authorization");

    private static final Set<String> EXPECTED = Set.of(
            "id", "correlationid", "operation", "accountid", "graphversion",
            "httpstatus", "success", "errorcode", "createdat");

    @Test
    void entityColumnsStayOperationalMetadataOnly() {
        Set<String> names = Arrays.stream(MetaApiCallLog.class.getDeclaredFields())
                .map(Field::getName)
                .map(n -> n.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        assertThat(names).isEqualTo(EXPECTED);
        assertNoForbiddenFragments(names);
    }

    @Test
    void apiResponseDtoStaysOperationalMetadataOnly() {
        Set<String> names = Arrays.stream(MetaApiCallLogResponse.class.getRecordComponents())
                .map(RecordComponent::getName)
                .map(n -> n.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        assertThat(names).isEqualTo(EXPECTED);
        assertNoForbiddenFragments(names);
    }

    @Test
    void constructorStoresOnlySafeFields() {
        MetaApiCallLog row = new MetaApiCallLog(
                "corr-1", "campaigns.list", "act_1", "v22.0", 200, true, null);
        assertThat(row.getCorrelationId()).isEqualTo("corr-1");
        assertThat(row.getOperation()).isEqualTo("campaigns.list");
        assertThat(row.getAccountId()).isEqualTo("act_1");
        assertThat(row.isSuccess()).isTrue();
        assertThat(row.getErrorCode()).isNull();
    }

    private static void assertNoForbiddenFragments(Set<String> names) {
        for (String name : names) {
            for (String forbidden : FORBIDDEN_FRAGMENTS) {
                assertThat(name)
                        .as("column %s must not look like sensitive storage", name)
                        .doesNotContain(forbidden);
            }
        }
    }
}
