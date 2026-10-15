package com.securetravels.crm.marketing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.securetravels.crm.common.config.AppProperties;
import com.securetravels.crm.common.exception.BadRequestException;
import com.securetravels.crm.common.exception.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MetaMarketingServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private MetaGraphClient graph;
    private MetaAdAccountRepository accounts;
    private MetaApiCallLogRepository callLogs;
    private AppProperties props;
    private MetaMarketingService service;

    @BeforeEach
    void setUp() {
        graph = mock(MetaGraphClient.class);
        accounts = mock(MetaAdAccountRepository.class);
        callLogs = mock(MetaApiCallLogRepository.class);
        props = new AppProperties();
        props.getMeta().setAccessToken("token");
        props.getMeta().setAdAccountIds("act_111");
        service = new MetaMarketingService(graph, accounts, callLogs, props);
        when(accounts.findByMetaAccountId(anyString())).thenReturn(Optional.empty());
        when(accounts.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void listAdAccountsFiltersToAllowlist() {
        ObjectNode a = mapper.createObjectNode().put("id", "act_111").put("name", "TOH").put("currency", "INR");
        ObjectNode b = mapper.createObjectNode().put("id", "act_999").put("name", "Other");
        ArrayNode data = mapper.createArrayNode().add(a).add(b);
        ObjectNode body = mapper.createObjectNode().set("data", data);

        when(graph.getAllPages(eq("adaccounts.list"), isNull(), eq("me/adaccounts"), anyMap(), anyInt(), any()))
                .thenAnswer(inv -> {
                    @SuppressWarnings("unchecked")
                    Function<com.fasterxml.jackson.databind.JsonNode, java.util.Iterator<com.fasterxml.jackson.databind.JsonNode>> extractor =
                            inv.getArgument(5);
                    java.util.List<com.fasterxml.jackson.databind.JsonNode> out = new java.util.ArrayList<>();
                    extractor.apply(body).forEachRemaining(out::add);
                    return out;
                });

        var list = service.listAdAccounts();
        assertThat(list).hasSize(1);
        assertThat(list.get(0).id()).isEqualTo("act_111");
        verify(accounts).save(any());
    }

    @Test
    void unauthorizedAccountIsForbidden() {
        assertThatThrownBy(() -> service.listCampaigns("act_999", null, 10))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void insightsRejectsPartialDateRange() {
        assertThatThrownBy(() -> service.insights("act_111", null, "2026-01-01", null, "campaign"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void usageSummaryReturnsSanitizedRows() {
        MetaApiCallLog row = new MetaApiCallLog(
                "c1", "campaigns.list", "act_111", "v22.0", 200, true, null);
        when(callLogs.countSince(any(Instant.class), eq(true))).thenReturn(3L);
        when(callLogs.countSince(any(Instant.class), eq(false))).thenReturn(1L);
        when(callLogs.findByCreatedAtGreaterThanEqualOrderByCreatedAtDesc(any(Instant.class), any()))
                .thenReturn(List.of(row));

        var summary = service.usageSummary(15, 50);
        assertThat(summary.successCount()).isEqualTo(3);
        assertThat(summary.failureCount()).isEqualTo(1);
        assertThat(summary.recent()).hasSize(1);
        assertThat(summary.recent().get(0).operation()).isEqualTo("campaigns.list");
        assertThat(summary.recent().get(0).correlationId()).isEqualTo("c1");
    }

    @Test
    void listCampaignsMapsFields() {
        ObjectNode c = mapper.createObjectNode()
                .put("id", "cmp1")
                .put("name", "Kedarnath")
                .put("status", "ACTIVE")
                .put("effective_status", "ACTIVE")
                .put("objective", "OUTCOME_LEADS")
                .put("updated_time", "2026-10-01T00:00:00+0000");
        ObjectNode body = mapper.createObjectNode();
        body.putArray("data").add(c);
        when(graph.get(eq("campaigns.list"), eq("act_111"), eq("act_111/campaigns"), anyMap()))
                .thenReturn(body);

        var page = service.listCampaigns("act_111", null, 25);
        assertThat(page.data()).hasSize(1);
        assertThat(page.data().get(0).name()).isEqualTo("Kedarnath");
        assertThat(page.hasMore()).isFalse();
    }
}
