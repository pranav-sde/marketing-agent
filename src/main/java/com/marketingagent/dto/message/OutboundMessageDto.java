package com.marketingagent.dto.message;

import com.marketingagent.domain.message.OutboundMessage;
import com.marketingagent.domain.message.OutboundMessageStatus;
import java.time.Instant;
import java.util.UUID;

public record OutboundMessageDto(
        UUID id,
        UUID tenantId,
        UUID broadcastId,
        UUID contactId,
        String idempotencyKey,
        OutboundMessageStatus status,
        String providerMessageId,
        String payloadHash,
        String lastErrorCode,
        String lastErrorCategory,
        Instant sentAt
) {
    public static OutboundMessageDto from(OutboundMessage message) {
        return new OutboundMessageDto(
                message.getId(),
                message.getTenant().getId(),
                message.getBroadcast().getId(),
                message.getContact().getId(),
                message.getIdempotencyKey(),
                message.getStatus(),
                message.getProviderMessageId(),
                message.getPayloadHash(),
                message.getLastErrorCode(),
                message.getLastErrorCategory(),
                message.getSentAt()
        );
    }
}
