package com.securetravels.crm.marketing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securetravels.crm.common.config.AppProperties;
import com.securetravels.crm.common.exception.ServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import org.springframework.http.HttpStatus;

class MetaGraphClientTest {

    private MockRestServiceServer server;
    private MetaGraphClient client;
    private MetaApiCallLogRepository logs;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        logs = mock(MetaApiCallLogRepository.class);
        when(logs.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AppProperties props = new AppProperties();
        props.getMeta().setAccessToken("test-token-not-real");
        props.getMeta().setGraphBaseUrl("https://graph.facebook.com");
        props.getMeta().setGraphVersion("v22.0");
        props.getMeta().setMaxRetries(0);

        client = new MetaGraphClient(builder.build(), new ObjectMapper(), props, logs);
    }

    @Test
    @DisplayName("GET campaigns returns data and records a successful call log")
    void success() {
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/v22.0/act_1/campaigns")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"data\":[{\"id\":\"c1\",\"name\":\"Kedarnath\"}]}",
                        MediaType.APPLICATION_JSON));

        var body = client.get("campaigns.list", "act_1", "act_1/campaigns", Map.of("fields", "id,name"));
        assertThat(body.path("data").get(0).path("name").asText()).isEqualTo("Kedarnath");
        server.verify();
    }

    @Test
    @DisplayName("Graph error body becomes MetaGraphException and is not silently empty")
    void graphError() {
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/v22.0/me/adaccounts")))
                .andRespond(withSuccess(
                        "{\"error\":{\"message\":\"Invalid OAuth access token\",\"type\":\"OAuthException\",\"code\":190}}",
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.get("adaccounts.list", null, "me/adaccounts", Map.of()))
                .isInstanceOf(MetaGraphException.class)
                .satisfies(ex -> {
                    MetaGraphException m = (MetaGraphException) ex;
                    assertThat(m.retryable()).isFalse();
                    assertThat(m.errorCode()).contains("190");
                });
    }

    @Test
    @DisplayName("missing META_ACCESS_TOKEN fails closed")
    void missingToken() {
        AppProperties props = new AppProperties();
        MetaGraphClient bare = new MetaGraphClient(
                RestClient.create(), new ObjectMapper(), props, logs);
        assertThatThrownBy(() -> bare.get("x", null, "me", Map.of()))
                .isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    @DisplayName("HTTP 429 is marked retryable")
    void rateLimit() {
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/v22.0/act_1/insights")))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"message\":\"(#17) User request limit\",\"code\":17}}"));

        assertThatThrownBy(() -> client.get("insights.list", "act_1", "act_1/insights", Map.of()))
                .isInstanceOf(MetaGraphException.class)
                .satisfies(ex -> assertThat(((MetaGraphException) ex).retryable()).isTrue());
    }
}
