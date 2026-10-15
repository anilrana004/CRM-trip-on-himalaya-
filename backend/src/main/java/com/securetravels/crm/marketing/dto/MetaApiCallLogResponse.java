package com.securetravels.crm.marketing.dto;

import java.time.Instant;
import java.util.UUID;

/** Sanitized Ads API usage row — never includes tokens or lead field data. */
public record MetaApiCallLogResponse(
        UUID id,
        String correlationId,
        String operation,
        String accountId,
        String graphVersion,
        Integer httpStatus,
        boolean success,
        String errorCode,
        Instant createdAt
) {}
