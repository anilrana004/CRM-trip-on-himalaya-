package com.securetravels.crm.marketing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.securetravels.crm.common.config.AppProperties;
import com.securetravels.crm.marketing.dto.MetaDiagnosticsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MetaDiagnosticsTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private MetaGraphClient graph;
    private MetaMarketingService service;
    private AppProperties props;

    @BeforeEach
    void setUp() {
        graph = mock(MetaGraphClient.class);
        props = new AppProperties();
        service = new MetaMarketingService(
                graph, mock(MetaAdAccountRepository.class), mock(MetaApiCallLogRepository.class), props);
    }

    @Test
    void missingTokenIsConfigMissingWithoutGraphCall() {
        var report = service.diagnostics();
        assertThat(report.status()).isEqualTo(MetaDiagnosticsResponse.ConnectivityStatus.CONFIG_MISSING);
        assertThat(report.config().accessTokenSet()).isFalse();
        assertThat(report.message()).contains("META_ACCESS_TOKEN");
        assertThat(report.webhookConfigReady()).isFalse();
    }

    @Test
    void okWhenMeAndAdAccountsSucceed() {
        props.getMeta().setAccessToken("token");
        props.getMeta().setAppSecret("secret");
        props.getMeta().setWebhookVerifyToken("verify");

        ObjectNode me = mapper.createObjectNode().put("id", "user_1");
        ObjectNode accounts = mapper.createObjectNode();
        accounts.putArray("data").addObject().put("id", "act_1");

        when(graph.get(eq("diagnostics.me"), isNull(), eq("me"), anyMap())).thenReturn(me);
        when(graph.get(eq("diagnostics.adaccounts"), isNull(), eq("me/adaccounts"), anyMap()))
                .thenReturn(accounts);

        var report = service.diagnostics();
        assertThat(report.status()).isEqualTo(MetaDiagnosticsResponse.ConnectivityStatus.OK);
        assertThat(report.graphUserId()).isEqualTo("user_1");
        assertThat(report.visibleAdAccountCount()).isEqualTo(1);
        assertThat(report.webhookConfigReady()).isTrue();
        assertThat(report.config().accessTokenSet()).isTrue();
    }

    @Test
    void authFailureMappedFromOauthError() {
        props.getMeta().setAccessToken("bad");
        when(graph.get(eq("diagnostics.me"), isNull(), eq("me"), anyMap()))
                .thenThrow(new MetaGraphException(401, "OAuthException:190", "Invalid OAuth access token", false));

        var report = service.diagnostics();
        assertThat(report.status()).isEqualTo(MetaDiagnosticsResponse.ConnectivityStatus.AUTH_FAILURE);
        assertThat(report.message()).contains("Rotate META_ACCESS_TOKEN");
    }

    @Test
    void allowlistPathUsesSingleAccountProbe() {
        props.getMeta().setAccessToken("token");
        props.getMeta().setAdAccountIds("act_999");
        when(graph.get(eq("diagnostics.me"), isNull(), eq("me"), anyMap()))
                .thenReturn(mapper.createObjectNode().put("id", "u"));
        when(graph.get(eq("diagnostics.adaccount"), eq("act_999"), eq("act_999"), anyMap()))
                .thenReturn(mapper.createObjectNode().put("id", "act_999").put("name", "TOH"));

        var report = service.diagnostics();
        assertThat(report.status()).isEqualTo(MetaDiagnosticsResponse.ConnectivityStatus.OK);
        assertThat(report.visibleAdAccountCount()).isEqualTo(1);
        assertThat(report.config().adAccountAllowlistSet()).isTrue();
    }
}
