package com.securetravels.crm.marketing;

import com.securetravels.crm.common.exception.ForbiddenException;
import com.securetravels.crm.common.exception.ServiceUnavailableException;
import com.securetravels.crm.marketing.dto.MetaAdAccountResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Controller error mapping (Meta Graph → CRM exceptions). Role gates are
 * {@code @PreAuthorize} on the controller; CRM unauthorized access is covered
 * by Spring Security in integration environments.
 */
class MetaMarketingControllerTest {

    private MetaMarketingService marketing;
    private MetaMarketingController controller;

    @BeforeEach
    void setUp() {
        marketing = mock(MetaMarketingService.class);
        controller = new MetaMarketingController(marketing);
    }

    @Test
    void returnsAccountsOnSuccess() {
        when(marketing.listAdAccounts()).thenReturn(List.of(
                new MetaAdAccountResponse("act_1", "TOH", "INR", 1)));
        assertThat(controller.adAccounts()).hasSize(1);
    }

    @Test
    void mapsOauthFailureToForbidden() {
        when(marketing.listAdAccounts())
                .thenThrow(new MetaGraphException(401, "OAuthException:190", "Invalid OAuth access token", false));
        assertThatThrownBy(controller::adAccounts)
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("permissions or token");
    }

    @Test
    void mapsRetryableFailureToServiceUnavailable() {
        when(marketing.listAdAccounts())
                .thenThrow(new MetaGraphException(503, "TRANSPORT", "Meta Graph unreachable", true));
        assertThatThrownBy(controller::adAccounts)
                .isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void stripsAccessTokenFragmentsFromErrorMessage() {
        when(marketing.listAdAccounts())
                .thenThrow(new MetaGraphException(
                        400, "GraphError:1", "bad access_token=EAABleak&foo=1", false));
        assertThatThrownBy(controller::adAccounts)
                .isInstanceOf(Exception.class)
                .hasMessageNotContaining("EAABleak");
    }
}
