package com.marketingagent.repository;

import com.marketingagent.domain.message.MessageAttempt;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageAttemptRepository extends JpaRepository<MessageAttempt, UUID> {
    Optional<MessageAttempt> findByIdempotencyKey(String idempotencyKey);

    List<MessageAttempt> findByOutboundMessage_IdOrderByAttemptNumberAsc(UUID outboundMessageId);
}
