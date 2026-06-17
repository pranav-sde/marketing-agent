package com.marketingagent.repository;

import com.marketingagent.domain.broadcast.Broadcast;
import com.marketingagent.domain.broadcast.BroadcastStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BroadcastRepository extends JpaRepository<Broadcast, UUID> {
    List<Broadcast> findByTenant_IdAndStatus(UUID tenantId, BroadcastStatus status);

    List<Broadcast> findByStatusAndScheduledStartAtLessThanEqual(BroadcastStatus status, Instant scheduledStartAt);
}
