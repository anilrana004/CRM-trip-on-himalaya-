package com.securetravels.crm.marketing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "meta_api_call_log")
public class MetaApiCallLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "correlation_id", nullable = false, length = 64)
    private String correlationId;

    @Column(nullable = false, length = 80)
    private String operation;

    @Column(name = "account_id", length = 64)
    private String accountId;

    @Column(name = "graph_version", nullable = false, length = 16)
    private String graphVersion;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "error_code", length = 64)
    private String errorCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected MetaApiCallLog() {}

    public MetaApiCallLog(String correlationId, String operation, String accountId, String graphVersion,
                          Integer httpStatus, boolean success, String errorCode) {
        this.correlationId = correlationId;
        this.operation = operation;
        this.accountId = accountId;
        this.graphVersion = graphVersion;
        this.httpStatus = httpStatus;
        this.success = success;
        this.errorCode = errorCode;
    }

    public UUID getId() { return id; }
    public String getCorrelationId() { return correlationId; }
    public String getOperation() { return operation; }
    public boolean isSuccess() { return success; }
}
