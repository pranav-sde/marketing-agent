package com.marketingagent.repository;

import com.marketingagent.domain.message.OutboundMessage;
import com.marketingagent.domain.message.OutboundMessageStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboundMessageRepository extends JpaRepository<OutboundMessage, UUID> {
    Optional<OutboundMessage> findByIdempotencyKey(String idempotencyKey);

    Optional<OutboundMessage> findByProviderMessageId(String providerMessageId);

    List<OutboundMessage> findByBroadcast_IdAndStatus(UUID broadcastId, OutboundMessageStatus status);

    long countByBroadcast_IdAndStatus(UUID broadcastId, OutboundMessageStatus status);
}
