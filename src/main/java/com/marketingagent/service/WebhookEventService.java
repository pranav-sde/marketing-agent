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
    public int processPendingWebhooks(Instant processedAt) {
        var events = providerWebhookEventRepository.findByProcessedAtIsNullOrderByReceivedAtAsc();
        for (ProviderWebhookEvent event : events) {
            try {
                processWebhookPayload(event.getPayload());
            } catch (Exception e) {
                // Log and ignore to prevent one bad webhook from halting all processing
            }
            event.setProcessedAt(processedAt);
        }
        providerWebhookEventRepository.saveAll(events);
        return events.size();
    }

    private void processWebhookPayload(java.util.Map<String, Object> payload) {
        // Basic naive traversal of WhatsApp Webhook Payload to find "statuses"
        if (payload == null || !payload.containsKey("entry")) return;
        
        java.util.List<java.util.Map<String, Object>> entries = (java.util.List<java.util.Map<String, Object>>) payload.get("entry");
        for (java.util.Map<String, Object> entry : entries) {
            if (!entry.containsKey("changes")) continue;
            java.util.List<java.util.Map<String, Object>> changes = (java.util.List<java.util.Map<String, Object>>) entry.get("changes");
            for (java.util.Map<String, Object> change : changes) {
                java.util.Map<String, Object> value = (java.util.Map<String, Object>) change.get("value");
                if (value != null && value.containsKey("statuses")) {
                    java.util.List<java.util.Map<String, Object>> statuses = (java.util.List<java.util.Map<String, Object>>) value.get("statuses");
                    for (java.util.Map<String, Object> statusObj : statuses) {
                        String wamid = (String) statusObj.get("id");
                        String statusStr = (String) statusObj.get("status");
                        if (wamid != null && statusStr != null) {
                            updateOutboundMessageStatus(wamid, statusStr);
                        }
                    }
                }
            }
        }
    }

    @org.springframework.beans.factory.annotation.Autowired
    private com.marketingagent.repository.ContentOutboundMessageRepository contentOutboundMessageRepository;

    private void updateOutboundMessageStatus(String wamid, String status) {
        contentOutboundMessageRepository.findByProviderMessageId(wamid).ifPresent(msg -> {
            switch (status.toLowerCase()) {
                case "delivered":
                    msg.setStatus(com.marketingagent.domain.message.OutboundMessageStatus.DELIVERED);
                    break;
                case "read":
                    msg.setStatus(com.marketingagent.domain.message.OutboundMessageStatus.READ);
                    break;
                case "failed":
                    msg.setStatus(com.marketingagent.domain.message.OutboundMessageStatus.FAILED);
                    break;
                case "sent":
                    if (msg.getStatus() == com.marketingagent.domain.message.OutboundMessageStatus.PENDING) {
                        msg.setStatus(com.marketingagent.domain.message.OutboundMessageStatus.SENT);
                    }
                    break;
            }
            contentOutboundMessageRepository.save(msg);
        });
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
