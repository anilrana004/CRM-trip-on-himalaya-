package com.securetravels.crm.marketing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MetaLeadgenEventRepository extends JpaRepository<MetaLeadgenEvent, UUID> {
    Optional<MetaLeadgenEvent> findByMetaLeadId(String metaLeadId);
}
