package com.marketingagent.repository;

import com.marketingagent.domain.message.DeliveryEvent;
import com.marketingagent.domain.message.DeliveryEventType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryEventRepository extends JpaRepository<DeliveryEvent, UUID> {
    List<DeliveryEvent> findByOutboundMessage_IdOrderByEventTimeAsc(UUID outboundMessageId);

    long countByTenant_IdAndEventTypeAndEventTimeBetween(
            UUID tenantId,
            DeliveryEventType eventType,
            Instant start,
            Instant end
    );
}
