package com.securetravels.crm.marketing.dto;

/**
 * Non-sensitive Meta connectivity report. Never includes tokens, secrets,
 * or full Graph payloads.
 */
public record MetaDiagnosticsResponse(
        ConfigFlags config,
        ConnectivityStatus status,
        String message,
        String graphVersion,
        String graphUserId,
        Integer visibleAdAccountCount,
        boolean webhookConfigReady
) {
    public record ConfigFlags(
            boolean accessTokenSet,
            boolean appSecretSet,
            boolean webhookVerifyTokenSet,
            boolean appIdSet,
            boolean adAccountAllowlistSet
    ) {}

    public enum ConnectivityStatus {
        CONFIG_MISSING,
        AUTH_FAILURE,
        PERMISSION_FAILURE,
        ACCOUNT_INACCESSIBLE,
        RATE_LIMITED,
        UNAVAILABLE,
        OK
    }
}
