package com.marketingagent.repository;

import com.marketingagent.domain.broadcast.BroadcastBatch;
import com.marketingagent.domain.broadcast.BroadcastBatchStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BroadcastBatchRepository extends JpaRepository<BroadcastBatch, UUID> {
    List<BroadcastBatch> findByBroadcast_IdAndStatus(UUID broadcastId, BroadcastBatchStatus status);

    List<BroadcastBatch> findByStatusAndLeaseExpiresAtBefore(BroadcastBatchStatus status, Instant leaseExpiresAt);
}
