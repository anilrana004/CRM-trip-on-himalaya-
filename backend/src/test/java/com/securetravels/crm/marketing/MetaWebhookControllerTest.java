package com.securetravels.crm.marketing;

import com.securetravels.crm.common.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MetaWebhookControllerTest {

    private MetaLeadgenService leadgen;
    private MetaWebhookController controller;

    @BeforeEach
    void setUp() {
        leadgen = mock(MetaLeadgenService.class);
        controller = new MetaWebhookController(leadgen);
    }

    @Test
    void verifyDelegatesChallenge() {
        when(leadgen.verifySubscription("subscribe", "tok", "42")).thenReturn("42");
        assertThat(controller.verify("subscribe", "tok", "42").getBody()).isEqualTo("42");
    }

    @Test
    void receiveRejectsOversizedPayloadWithBadRequest() {
        byte[] huge = new byte[64 * 1024 + 1];
        assertThatThrownBy(() -> controller.receive(huge, "sha256=x"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("size limit");
    }

    @Test
    void receiveForwardsRawBodyAndSignature() {
        byte[] body = "{\"object\":\"page\"}".getBytes();
        controller.receive(body, "sha256=abc");
        verify(leadgen).ingest(eq(body), eq("sha256=abc"));
    }

    @Test
    void receiveTreatsNullBodyAsEmpty() {
        controller.receive(null, "sha256=abc");
        verify(leadgen).ingest(any(byte[].class), eq("sha256=abc"));
    }
}
