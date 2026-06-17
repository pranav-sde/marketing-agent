package com.marketingagent.dto.contact;

import com.marketingagent.domain.common.Channel;
import com.marketingagent.domain.contact.ConsentEvent;
import com.marketingagent.domain.contact.ConsentEventType;
import java.time.Instant;
import java.util.UUID;

public record ConsentEventDto(
        UUID id,
        UUID tenantId,
        UUID contactId,
        Channel channel,
        ConsentEventType eventType,
        String source,
        Instant capturedAt,
        String policyVersion,
        String evidenceRef
) {
    public static ConsentEventDto from(ConsentEvent event) {
        return new ConsentEventDto(
                event.getId(),
                event.getTenant().getId(),
                event.getContact().getId(),
                event.getChannel(),
                event.getEventType(),
                event.getSource(),
                event.getCapturedAt(),
                event.getPolicyVersion(),
                event.getEvidenceRef()
        );
    }
}
