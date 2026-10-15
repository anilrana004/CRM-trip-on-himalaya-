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
@Table(name = "meta_ad_accounts")
public class MetaAdAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "meta_account_id", nullable = false, unique = true, length = 64)
    private String metaAccountId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 16)
    private String currency;

    @Column(name = "account_status")
    private Integer accountStatus;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected MetaAdAccount() {}

    public MetaAdAccount(String metaAccountId, String name) {
        this.metaAccountId = metaAccountId;
        this.name = name;
    }

    public UUID getId() { return id; }
    public String getMetaAccountId() { return metaAccountId; }
    public String getName() { return name; }
    public String getCurrency() { return currency; }
    public Integer getAccountStatus() { return accountStatus; }
    public boolean isActive() { return active; }
    public Instant getLastSyncedAt() { return lastSyncedAt; }

    public void applySync(String name, String currency, Integer accountStatus) {
        this.name = name == null || name.isBlank() ? this.name : name;
        this.currency = currency;
        this.accountStatus = accountStatus;
        this.lastSyncedAt = Instant.now();
        this.updatedAt = Instant.now();
        this.active = true;
    }
}
