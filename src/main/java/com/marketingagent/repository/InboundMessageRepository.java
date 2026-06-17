package com.marketingagent.repository;

import com.marketingagent.domain.message.InboundMessage;
import com.marketingagent.domain.message.InboundMessageClassification;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InboundMessageRepository extends JpaRepository<InboundMessage, UUID> {
    List<InboundMessage> findByTenant_IdAndClassification(UUID tenantId, InboundMessageClassification classification);

    List<InboundMessage> findByTenant_IdAndReceivedAtBetween(UUID tenantId, Instant start, Instant end);
}
