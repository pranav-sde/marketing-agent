package com.marketingagent.service;

import com.marketingagent.domain.integration.WhatsAppAccount;
import com.marketingagent.domain.message.ProviderWebhookEvent;
import com.marketingagent.dto.message.WebhookIngestRequest;
import com.marketingagent.exception.ResourceNotFoundException;
import com.marketingagent.repository.ProviderWebhookEventRepository;
import com.marketingagent.repository.WhatsAppAccountRepository;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class WebhookEventService {

    private final ProviderWebhookEventRepository providerWebhookEventRepository;
    private final WhatsAppAccountRepository whatsAppAccountRepository;

    public WebhookEventService(
            ProviderWebhookEventRepository providerWebhookEventRepository,
            WhatsAppAccountRepository whatsAppAccountRepository
    ) {
        this.providerWebhookEventRepository = providerWebhookEventRepository;
        this.whatsAppAccountRepository = whatsAppAccountRepository;
    }

    @Transactional
    public UUID ingestWebhook(@Valid WebhookIngestRequest request) {
        return providerWebhookEventRepository.findByEventHash(request.eventHash())
                .map(ProviderWebhookEvent::getId)
                .orElseGet(() -> persistWebhook(request).getId());
    }

    @Transactional
    public int markPendingWebhooksProcessed(Instant processedAt) {
        var events = providerWebhookEventRepository.findByProcessedAtIsNullOrderByReceivedAtAsc();
        events.forEach(event -> event.setProcessedAt(processedAt));
        providerWebhookEventRepository.saveAll(events);
        return events.size();
    }

    private ProviderWebhookEvent persistWebhook(WebhookIngestRequest request) {
        ProviderWebhookEvent event = new ProviderWebhookEvent(
                request.provider(),
                request.eventHash(),
                Instant.now(),
                request.payload()
        );
        event.setProviderEventId(request.providerEventId());
        if (request.whatsAppAccountId() != null) {
            WhatsAppAccount account = whatsAppAccountRepository.findById(request.whatsAppAccountId())
                    .orElseThrow(() -> new ResourceNotFoundException("WhatsAppAccount", request.whatsAppAccountId()));
            event.setWhatsAppAccount(account);
            event.setTenant(account.getTenant());
        }
        return providerWebhookEventRepository.save(event);
    }
}
