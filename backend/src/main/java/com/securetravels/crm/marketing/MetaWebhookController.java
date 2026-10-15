package com.securetravels.crm.marketing;

import com.securetravels.crm.common.exception.BadRequestException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public Meta Lead Ads webhook. JWT is not used — authentication is hub verify
 * token (GET) and {@code X-Hub-Signature-256} (POST).
 */
@RestController
@RequestMapping("/api/webhook/meta")
public class MetaWebhookController {

    private static final int MAX_PAYLOAD_BYTES = 64 * 1024;

    private final MetaLeadgenService leadgen;

    public MetaWebhookController(MetaLeadgenService leadgen) {
        this.leadgen = leadgen;
    }

    @Operation(summary = "Meta webhook subscription verification")
    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> verify(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String verifyToken,
            @RequestParam(name = "hub.challenge", required = false) String challenge) {
        return ResponseEntity.ok(leadgen.verifySubscription(mode, verifyToken, challenge));
    }

    @Operation(summary = "Meta leadgen webhook receiver")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Accepted (including duplicates / ignored fields)"),
            @ApiResponse(responseCode = "401", description = "Invalid signature"),
            @ApiResponse(responseCode = "429", description = "Rate limited")
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> receive(
            @RequestBody byte[] raw,
            @RequestHeader(value = MetaHubSignature.HEADER, required = false) String signature) {
        if (raw != null && raw.length > MAX_PAYLOAD_BYTES) {
            // Not a signature failure — avoid 401 so operators can distinguish misconfig from abuse.
            throw new BadRequestException("Meta webhook payload exceeds size limit");
        }
        leadgen.ingest(raw == null ? new byte[0] : raw, signature);
        return ResponseEntity.ok().build();
    }
}
