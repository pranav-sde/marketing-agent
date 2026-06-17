package com.marketingagent.service;

import com.marketingagent.domain.common.Channel;
import com.marketingagent.domain.contact.ConsentEvent;
import com.marketingagent.domain.contact.ConsentEventType;
import com.marketingagent.domain.contact.Contact;
import com.marketingagent.domain.contact.ContactChannelState;
import com.marketingagent.domain.contact.ContactStatus;
import com.marketingagent.domain.contact.SuppressionListEntry;
import com.marketingagent.domain.contact.SuppressionReason;
import com.marketingagent.dto.contact.ConsentEventDto;
import com.marketingagent.dto.contact.ConsentRecordRequest;
import com.marketingagent.repository.ConsentEventRepository;
import com.marketingagent.repository.ContactChannelStateRepository;
import com.marketingagent.repository.ContactRepository;
import com.marketingagent.repository.SuppressionListEntryRepository;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class ConsentService {

    private final ContactService contactService;
    private final ConsentEventRepository consentEventRepository;
    private final ContactChannelStateRepository contactChannelStateRepository;
    private final ContactRepository contactRepository;
    private final SuppressionListEntryRepository suppressionListEntryRepository;

    public ConsentService(
            ContactService contactService,
            ConsentEventRepository consentEventRepository,
            ContactChannelStateRepository contactChannelStateRepository,
            ContactRepository contactRepository,
            SuppressionListEntryRepository suppressionListEntryRepository
    ) {
        this.contactService = contactService;
        this.consentEventRepository = consentEventRepository;
        this.contactChannelStateRepository = contactChannelStateRepository;
        this.contactRepository = contactRepository;
        this.suppressionListEntryRepository = suppressionListEntryRepository;
    }

    @Transactional
    public ConsentEventDto recordConsent(UUID tenantId, UUID contactId, @Valid ConsentRecordRequest request) {
        Contact contact = contactService.getContactEntity(tenantId, contactId);
        ConsentEvent event = new ConsentEvent(
                contact.getTenant(),
                contact,
                request.channel(),
                request.eventType(),
                request.source(),
                request.capturedAt()
        );
        event.setPolicyVersion(request.policyVersion());
        event.setEvidenceRef(request.evidenceRef());

        ContactChannelState channelState = contactChannelStateRepository
                .findByContact_IdAndChannel(contactId, request.channel())
                .orElseGet(() -> new ContactChannelState(contact.getTenant(), contact, request.channel()));
        channelState.setStatus(resolveContactStatus(request.eventType()));
        channelState.setLastConsentEventAt(request.capturedAt());
        contactChannelStateRepository.save(channelState);

        if (request.eventType() == ConsentEventType.OPT_OUT || request.eventType() == ConsentEventType.ADMIN_SUPPRESS) {
            contact.setStatus(ContactStatus.OPTED_OUT);
            contactRepository.save(contact);
            createSuppressionIfMissing(contact, request.channel(), request.source());
        }

        return ConsentEventDto.from(consentEventRepository.save(event));
    }

    @Transactional(readOnly = true)
    public List<ConsentEventDto> listConsentEvents(UUID tenantId, UUID contactId) {
        contactService.getContactEntity(tenantId, contactId);
        return consentEventRepository.findByTenant_IdAndContact_IdOrderByCapturedAtDesc(tenantId, contactId)
                .stream()
                .map(ConsentEventDto::from)
                .toList();
    }

    public boolean isSuppressed(UUID tenantId, Channel channel, String phoneHash) {
        return suppressionListEntryRepository.existsByTenant_IdAndChannelAndPhoneHash(tenantId, channel, phoneHash);
    }

    private ContactStatus resolveContactStatus(ConsentEventType eventType) {
        return switch (eventType) {
            case OPT_IN, ADMIN_RESTORE -> ContactStatus.ACTIVE;
            case OPT_OUT, ADMIN_SUPPRESS -> ContactStatus.OPTED_OUT;
            case DATA_ERASURE -> ContactStatus.DELETED;
        };
    }

    private void createSuppressionIfMissing(Contact contact, Channel channel, String source) {
        suppressionListEntryRepository.findByTenant_IdAndChannelAndPhoneHash(
                contact.getTenant().getId(),
                channel,
                contact.getPhoneHash()
        ).ifPresentOrElse(existing -> { }, () -> {
            SuppressionListEntry entry = new SuppressionListEntry(
                    contact.getTenant(),
                    channel,
                    contact.getPhoneHash(),
                    SuppressionReason.OPT_OUT,
                    java.time.Instant.now()
            );
            entry.setSource(source);
            suppressionListEntryRepository.save(entry);
        });
    }
}
