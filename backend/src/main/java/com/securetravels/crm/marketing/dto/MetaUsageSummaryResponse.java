package com.securetravels.crm.marketing.dto;

import java.util.List;

public record MetaUsageSummaryResponse(
        long successCount,
        long failureCount,
        InstantWindow window,
        List<MetaApiCallLogResponse> recent
) {
    public record InstantWindow(java.time.Instant from, java.time.Instant to) {}
}
