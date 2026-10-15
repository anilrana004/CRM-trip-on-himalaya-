package com.securetravels.crm.marketing;

import com.securetravels.crm.BaseIT;
import com.securetravels.crm.user.Role;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CRM role gate for Meta marketing APIs. Does not call live Graph — token is
 * unset in the test profile, so authorized managers receive 503 (config), while
 * SALES receives 403 (forbidden) before the service runs.
 */
class MetaMarketingAccessIT extends BaseIT {

    @Test
    void salesCannotAccessMetaMarketingEndpoints() throws Exception {
        createUser("sales.meta@toh.test", "Sales Meta", Role.SALES, "sales12345");
        String token = login("sales.meta@toh.test", "sales12345");

        mockMvc.perform(get("/api/marketing/meta/ad-accounts").header("Authorization", authHeader(token)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/marketing/meta/usage").header("Authorization", authHeader(token)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/marketing/meta/diagnostics").header("Authorization", authHeader(token)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/marketing/meta/ad-accounts/act_1/campaigns")
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/marketing/meta/ad-accounts/act_1/insights")
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void managerWithoutMetaTokenGetsServiceUnavailableOnAdsList() throws Exception {
        createUser("mgr.meta@toh.test", "Manager Meta", Role.MANAGER, "manager12345");
        String token = login("mgr.meta@toh.test", "manager12345");

        mockMvc.perform(get("/api/marketing/meta/ad-accounts").header("Authorization", authHeader(token)))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void managerCanReadUsageEvidenceWithoutMetaToken() throws Exception {
        createUser("mgr.usage@toh.test", "Manager Usage", Role.MANAGER, "manager12345");
        String token = login("mgr.usage@toh.test", "manager12345");

        // Usage is CRM-side metadata; it must not require META_ACCESS_TOKEN.
        mockMvc.perform(get("/api/marketing/meta/usage?days=15&limit=20")
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk());
    }
}
