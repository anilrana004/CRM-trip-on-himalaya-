package com.securetravels.crm.marketing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MetaAdAccountRepository extends JpaRepository<MetaAdAccount, UUID> {
    Optional<MetaAdAccount> findByMetaAccountId(String metaAccountId);
}
