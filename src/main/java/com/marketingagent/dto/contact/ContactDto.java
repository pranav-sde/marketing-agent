package com.marketingagent.dto.contact;

import com.marketingagent.domain.contact.Contact;
import com.marketingagent.domain.contact.ContactStatus;
import java.time.Instant;
import java.util.UUID;

public record ContactDto(
        UUID id,
        UUID tenantId,
        String phoneE164,
        String phoneHash,
        String firstName,
        String lastName,
        String email,
        ContactStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static ContactDto from(Contact contact) {
        return new ContactDto(
                contact.getId(),
                contact.getTenant().getId(),
                contact.getPhoneE164(),
                contact.getPhoneHash(),
                contact.getFirstName(),
                contact.getLastName(),
                contact.getEmail(),
                contact.getStatus(),
                contact.getCreatedAt(),
                contact.getUpdatedAt()
        );
    }
}
