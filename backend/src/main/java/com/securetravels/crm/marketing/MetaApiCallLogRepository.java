package com.securetravels.crm.marketing;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MetaApiCallLogRepository extends JpaRepository<MetaApiCallLog, UUID> {

    List<MetaApiCallLog> findByCreatedAtGreaterThanEqualOrderByCreatedAtDesc(Instant from, Pageable pageable);

    @Query("""
            select count(l) from MetaApiCallLog l
            where l.createdAt >= :from and l.success = :success
            """)
    long countSince(@Param("from") Instant from, @Param("success") boolean success);
}
