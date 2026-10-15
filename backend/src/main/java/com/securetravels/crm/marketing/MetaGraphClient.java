package com.securetravels.crm.marketing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.securetravels.crm.common.config.AppProperties;
import com.securetravels.crm.common.exception.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Thin Graph API client. Never logs access tokens. Retries only transient
 * failures (429 / 5xx) up to {@code app.meta.max-retries}.
 */
@Component
public class MetaGraphClient {

    private static final Logger log = LoggerFactory.getLogger(MetaGraphClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final AppProperties props;
    private final MetaApiCallLogRepository callLogs;

    public MetaGraphClient(RestClient metaRestClient, ObjectMapper objectMapper,
                           AppProperties props, MetaApiCallLogRepository callLogs) {
        this.restClient = metaRestClient;
        this.objectMapper = objectMapper;
        this.props = props;
        this.callLogs = callLogs;
    }

    public JsonNode get(String operation, String accountId, String relativePath, Map<String, String> query) {
        requireConfigured();
        String correlationId = UUID.randomUUID().toString();
        int attempts = 0;
        int maxAttempts = Math.max(1, props.getMeta().getMaxRetries() + 1);
        MetaGraphException last = null;

        while (attempts < maxAttempts) {
            attempts++;
            try {
                return executeGet(operation, accountId, relativePath, query, correlationId);
            } catch (MetaGraphException ex) {
                last = ex;
                if (!ex.retryable() || attempts >= maxAttempts) {
                    throw ex;
                }
                sleepBackoff(attempts);
            }
        }
        throw last != null ? last : new MetaGraphException(500, "UNKNOWN", "Meta request failed", false);
    }

    public List<JsonNode> getAllPages(String operation, String accountId, String relativePath,
                                      Map<String, String> query, int maxPages,
                                      Function<JsonNode, Iterator<JsonNode>> dataExtractor) {
        List<JsonNode> out = new ArrayList<>();
        Map<String, String> pageQuery = new LinkedHashMap<>(query == null ? Map.of() : query);
        int pages = 0;
        while (pages < maxPages) {
            pages++;
            JsonNode body = get(operation, accountId, relativePath, pageQuery);
            Iterator<JsonNode> data = dataExtractor.apply(body);
            if (data != null) {
                data.forEachRemaining(out::add);
            }
            String after = body.path("paging").path("cursors").path("after").asText(null);
            boolean hasNext = body.path("paging").hasNonNull("next");
            if (!hasNext || after == null || after.isBlank()) {
                break;
            }
            pageQuery.put("after", after);
        }
        return out;
    }

    private JsonNode executeGet(String operation, String accountId, String relativePath,
                                Map<String, String> query, String correlationId) {
        URI uri = buildUri(relativePath, query);
        try {
            ResponseEntity<String> response = restClient.get()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange((req, res) -> ResponseEntity.status(res.getStatusCode())
                            .headers(res.getHeaders())
                            .body(new String(res.getBody().readAllBytes())));

            int status = response.getStatusCode().value();
            JsonNode body = objectMapper.readTree(response.getBody() == null ? "{}" : response.getBody());
            if (!response.getStatusCode().is2xxSuccessful() || body.has("error")) {
                JsonNode err = body.path("error");
                int code = err.path("code").asInt(status);
                String type = err.path("type").asText("GraphError");
                String message = err.path("message").asText("Meta Graph error (HTTP " + status + ")");
                boolean retryable = status == 429 || status >= 500 || code == 4 || code == 17 || code == 32;
                record(correlationId, operation, accountId, status, false, String.valueOf(code));
                throw new MetaGraphException(status, type + ":" + code, message, retryable);
            }
            record(correlationId, operation, accountId, status, true, null);
            return body;
        } catch (MetaGraphException ex) {
            throw ex;
        } catch (RestClientException ex) {
            record(correlationId, operation, accountId, null, false, "TRANSPORT");
            log.warn("[meta] transport failure op={} correlationId={}", operation, correlationId);
            throw new MetaGraphException(502, "TRANSPORT", "Meta Graph unreachable", true);
        } catch (Exception ex) {
            record(correlationId, operation, accountId, null, false, "PARSE");
            throw new MetaGraphException(502, "PARSE", "Meta Graph response could not be parsed", false);
        }
    }

    private URI buildUri(String relativePath, Map<String, String> query) {
        String base = trimSlash(props.getMeta().getGraphBaseUrl());
        String version = props.getMeta().getGraphVersion();
        if (version == null || version.isBlank()) {
            version = "v22.0";
        }
        String path = relativePath.startsWith("/") ? relativePath.substring(1) : relativePath;
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(base + "/" + version + "/" + path)
                .queryParam("access_token", props.getMeta().getAccessToken());
        if (query != null) {
            query.forEach((k, v) -> {
                if (v != null && !v.isBlank()) {
                    builder.queryParam(k, v);
                }
            });
        }
        return builder.encode().build().toUri();
    }

    private void record(String correlationId, String operation, String accountId,
                        Integer httpStatus, boolean success, String errorCode) {
        try {
            callLogs.save(new MetaApiCallLog(
                    correlationId,
                    operation,
                    accountId,
                    props.getMeta().getGraphVersion(),
                    httpStatus,
                    success,
                    errorCode));
        } catch (Exception ex) {
            log.warn("[meta] failed to persist api call log op={}", operation);
        }
    }

    private void requireConfigured() {
        if (!props.getMeta().configured()) {
            throw new ServiceUnavailableException(
                    "Meta Marketing API is not configured. Set META_ACCESS_TOKEN on the server.");
        }
    }

    private static void sleepBackoff(int attempt) {
        try {
            Thread.sleep(200L * attempt);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private static String trimSlash(String value) {
        if (value == null || value.isBlank()) {
            return "https://graph.facebook.com";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
