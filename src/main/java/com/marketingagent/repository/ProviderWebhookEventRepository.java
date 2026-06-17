package com.marketingagent.repository;

import com.marketingagent.domain.message.ProviderWebhookEvent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProviderWebhookEventRepository extends JpaRepository<ProviderWebhookEvent, UUID> {
    Optional<ProviderWebhookEvent> findByEventHash(String eventHash);

    Optional<ProviderWebhookEvent> findByProviderEventId(String providerEventId);

    List<ProviderWebhookEvent> findByProcessedAtIsNullOrderByReceivedAtAsc();
}
