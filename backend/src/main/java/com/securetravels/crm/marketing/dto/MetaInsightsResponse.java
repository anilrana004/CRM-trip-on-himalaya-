package com.securetravels.crm.marketing.dto;

import java.util.List;

public record MetaInsightsResponse(
        String accountId,
        String datePreset,
        String since,
        String until,
        List<MetaInsightRow> rows
) {
    public record MetaInsightRow(
            String campaignId,
            String campaignName,
            String impressions,
            String clicks,
            String spend,
            String cpc,
            String cpm,
            String ctr,
            String dateStart,
            String dateStop
    ) {}
}
