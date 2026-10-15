package com.securetravels.crm.marketing.dto;

import java.util.List;

public record MetaPageResponse<T>(
        List<T> data,
        String nextCursor,
        boolean hasMore
) {}
