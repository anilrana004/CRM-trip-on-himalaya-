package com.securetravels.crm.marketing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MetaApiCallLogRepository extends JpaRepository<MetaApiCallLog, UUID> {
}
