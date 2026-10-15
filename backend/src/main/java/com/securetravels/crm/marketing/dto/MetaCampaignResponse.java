package com.securetravels.crm.marketing.dto;

public record MetaCampaignResponse(
        String id,
        String name,
        String status,
        String effectiveStatus,
        String objective,
        String updatedTime
) {}
