package com.securetravels.crm.marketing;

import com.fasterxml.jackson.databind.JsonNode;
import com.securetravels.crm.common.config.AppProperties;
import com.securetravels.crm.common.exception.BadRequestException;
import com.securetravels.crm.common.exception.ForbiddenException;
import com.securetravels.crm.common.exception.ServiceUnavailableException;
import com.securetravels.crm.marketing.dto.MetaAdAccountResponse;
import com.securetravels.crm.marketing.dto.MetaApiCallLogResponse;
import com.securetravels.crm.marketing.dto.MetaCampaignResponse;
import com.securetravels.crm.marketing.dto.MetaInsightsResponse;
import com.securetravels.crm.marketing.dto.MetaPageResponse;
import com.securetravels.crm.marketing.dto.MetaUsageSummaryResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MetaMarketingService {

    private static final int MAX_PAGES = 20;
    private static final String CAMPAIGN_FIELDS = "id,name,status,effective_status,objective,updated_time";
    private static final String INSIGHT_FIELDS =
            "campaign_id,campaign_name,impressions,clicks,spend,cpc,cpm,ctr,date_start,date_stop";

    private final MetaGraphClient graph;
    private final MetaAdAccountRepository accounts;
    private final MetaApiCallLogRepository callLogs;
    private final AppProperties props;

    public MetaMarketingService(MetaGraphClient graph, MetaAdAccountRepository accounts,
                                MetaApiCallLogRepository callLogs, AppProperties props) {
        this.graph = graph;
        this.accounts = accounts;
        this.callLogs = callLogs;
        this.props = props;
    }

    @Transactional(readOnly = true)
    public MetaUsageSummaryResponse usageSummary(int days, int limit) {
        int windowDays = Math.min(Math.max(days, 1), 90);
        int rowLimit = Math.min(Math.max(limit, 1), 100);
        Instant from = Instant.now().minus(windowDays, ChronoUnit.DAYS);
        Instant to = Instant.now();
        long ok = callLogs.countSince(from, true);
        long fail = callLogs.countSince(from, false);
        List<MetaApiCallLogResponse> recent = callLogs
                .findByCreatedAtGreaterThanEqualOrderByCreatedAtDesc(from, PageRequest.of(0, rowLimit))
                .stream()
                .map(l -> new MetaApiCallLogResponse(
                        l.getId(),
                        l.getCorrelationId(),
                        l.getOperation(),
                        l.getAccountId(),
                        l.getGraphVersion(),
                        l.getHttpStatus(),
                        l.isSuccess(),
                        l.getErrorCode(),
                        l.getCreatedAt()))
                .toList();
        return new MetaUsageSummaryResponse(
                ok, fail, new MetaUsageSummaryResponse.InstantWindow(from, to), recent);
    }

    @Transactional
    public List<MetaAdAccountResponse> listAdAccounts() {
        requireConfigured();
        Map<String, String> query = Map.of(
                "fields", "id,account_id,name,currency,account_status",
                "limit", "50");
        List<JsonNode> nodes = graph.getAllPages(
                "adaccounts.list",
                null,
                "me/adaccounts",
                query,
                MAX_PAGES,
                body -> body.path("data").elements());

        Set<String> allow = configuredAllowlist();
        List<MetaAdAccountResponse> out = new ArrayList<>();
        for (JsonNode n : nodes) {
            String id = normalizeAccountId(firstNonBlank(n.path("id").asText(null), n.path("account_id").asText(null)));
            if (id == null) {
                continue;
            }
            if (!allow.isEmpty() && !allow.contains(id) && !allow.contains(stripAct(id))) {
                continue;
            }
            String name = n.path("name").asText(id);
            String currency = textOrNull(n, "currency");
            Integer status = n.hasNonNull("account_status") ? n.get("account_status").asInt() : null;
            upsertLocal(id, name, currency, status);
            out.add(new MetaAdAccountResponse(id, name, currency, status));
        }
        return out;
    }

    public MetaPageResponse<MetaCampaignResponse> listCampaigns(String accountId, String after, int limit) {
        requireConfigured();
        String act = assertAuthorizedAccount(accountId);
        int pageSize = Math.min(Math.max(limit, 1), 100);
        Map<String, String> query = new LinkedHashMap<>();
        query.put("fields", CAMPAIGN_FIELDS);
        query.put("limit", String.valueOf(pageSize));
        if (after != null && !after.isBlank()) {
            query.put("after", after);
        }

        JsonNode body = graph.get("campaigns.list", act, act + "/campaigns", query);
        List<MetaCampaignResponse> rows = new ArrayList<>();
        body.path("data").forEach(n -> rows.add(new MetaCampaignResponse(
                textOrNull(n, "id"),
                textOrNull(n, "name"),
                textOrNull(n, "status"),
                textOrNull(n, "effective_status"),
                textOrNull(n, "objective"),
                textOrNull(n, "updated_time"))));
        String next = body.path("paging").path("cursors").path("after").asText(null);
        boolean hasMore = body.path("paging").hasNonNull("next");
        return new MetaPageResponse<>(rows, hasMore ? next : null, hasMore);
    }

    public MetaInsightsResponse insights(String accountId, String datePreset, String since, String until,
                                         String level) {
        requireConfigured();
        String act = assertAuthorizedAccount(accountId);

        boolean hasRange = notBlank(since) || notBlank(until);
        if (hasRange && (isBlank(since) || isBlank(until))) {
            throw new BadRequestException("Both since and until are required for a custom date range (YYYY-MM-DD)");
        }
        if (hasRange && (notBlank(datePreset))) {
            throw new BadRequestException("Provide either datePreset or since/until, not both");
        }

        Map<String, String> query = new LinkedHashMap<>();
        query.put("fields", INSIGHT_FIELDS);
        query.put("level", level == null || level.isBlank() ? "campaign" : level);
        query.put("limit", "100");
        if (hasRange) {
            query.put("time_range", "{\"since\":\"" + since + "\",\"until\":\"" + until + "\"}");
        } else {
            query.put("date_preset", notBlank(datePreset) ? datePreset : "last_7d");
        }

        List<JsonNode> nodes = graph.getAllPages(
                "insights.list",
                act,
                act + "/insights",
                query,
                MAX_PAGES,
                body -> body.path("data").elements());

        List<MetaInsightsResponse.MetaInsightRow> rows = new ArrayList<>();
        for (JsonNode n : nodes) {
            rows.add(new MetaInsightsResponse.MetaInsightRow(
                    textOrNull(n, "campaign_id"),
                    textOrNull(n, "campaign_name"),
                    textOrNull(n, "impressions"),
                    textOrNull(n, "clicks"),
                    textOrNull(n, "spend"),
                    textOrNull(n, "cpc"),
                    textOrNull(n, "cpm"),
                    textOrNull(n, "ctr"),
                    textOrNull(n, "date_start"),
                    textOrNull(n, "date_stop")));
        }
        return new MetaInsightsResponse(
                act,
                hasRange ? null : query.get("date_preset"),
                hasRange ? since : null,
                hasRange ? until : null,
                rows);
    }

    private void upsertLocal(String id, String name, String currency, Integer status) {
        MetaAdAccount entity = accounts.findByMetaAccountId(id)
                .orElseGet(() -> new MetaAdAccount(id, name));
        entity.applySync(name, currency, status);
        accounts.save(entity);
    }

    String assertAuthorizedAccount(String raw) {
        String act = normalizeAccountId(raw);
        if (act == null) {
            throw new BadRequestException("ad account id is required");
        }
        Set<String> allow = configuredAllowlist();
        if (!allow.isEmpty()) {
            if (!allow.contains(act) && !allow.contains(stripAct(act))) {
                throw new ForbiddenException("Ad account is not authorized for this CRM integration");
            }
            return act;
        }
        // No static allowlist: require the account to have been discovered/synced,
        // or accept act_ form and verify via a cheap Graph read on first use.
        if (accounts.findByMetaAccountId(act).isPresent()) {
            return act;
        }
        JsonNode node = graph.get("adaccount.get", act, act, Map.of("fields", "id,name"));
        String id = normalizeAccountId(node.path("id").asText(act));
        upsertLocal(id, node.path("name").asText(id), null, null);
        return id;
    }

    private Set<String> configuredAllowlist() {
        String raw = props.getMeta().getAdAccountIds();
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(this::normalizeAccountId)
                .filter(s -> s != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void requireConfigured() {
        if (!props.getMeta().configured()) {
            throw new ServiceUnavailableException(
                    "Meta Marketing API is not configured. Set META_ACCESS_TOKEN on the server.");
        }
    }

    private String normalizeAccountId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.toLowerCase(Locale.ROOT).startsWith("act_")) {
            return "act_" + trimmed.substring(4);
        }
        if (trimmed.chars().allMatch(Character::isDigit)) {
            return "act_" + trimmed;
        }
        return trimmed;
    }

    private static String stripAct(String id) {
        return id != null && id.startsWith("act_") ? id.substring(4) : id;
    }

    private static String textOrNull(JsonNode n, String field) {
        JsonNode v = n.get(field);
        if (v == null || v.isNull()) {
            return null;
        }
        String text = v.asText();
        return text == null || text.isBlank() ? null : text;
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        if (b != null && !b.isBlank()) {
            return b;
        }
        return null;
    }

    private static boolean notBlank(String v) {
        return v != null && !v.isBlank();
    }

    private static boolean isBlank(String v) {
        return v == null || v.isBlank();
    }
}
