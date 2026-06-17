package com.marketingagent.service;

import com.marketingagent.domain.contact.Contact;
import com.marketingagent.domain.tenant.Tenant;
import com.marketingagent.dto.contact.ContactCreateRequest;
import com.marketingagent.dto.contact.ContactDto;
import com.marketingagent.exception.ConflictException;
import com.marketingagent.exception.ResourceNotFoundException;
import com.marketingagent.repository.ContactRepository;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class ContactService {

    private final ContactRepository contactRepository;
    private final TenantService tenantService;
    private final PhoneHashingService phoneHashingService;

    public ContactService(
            ContactRepository contactRepository,
            TenantService tenantService,
            PhoneHashingService phoneHashingService
    ) {
        this.contactRepository = contactRepository;
        this.tenantService = tenantService;
        this.phoneHashingService = phoneHashingService;
    }

    @Transactional
    public ContactDto createContact(UUID tenantId, @Valid ContactCreateRequest request) {
        contactRepository.findByTenant_IdAndPhoneE164(tenantId, request.phoneE164()).ifPresent(existing -> {
            throw new ConflictException("Contact already exists for phone number");
        });

        Tenant tenant = tenantService.getTenantEntity(tenantId);
        Contact contact = new Contact(tenant, request.phoneE164(), phoneHashingService.hashPhone(request.phoneE164()));
        contact.setFirstName(request.firstName());
        contact.setLastName(request.lastName());
        contact.setEmail(request.email());
        return ContactDto.from(contactRepository.save(contact));
    }

    @Transactional(readOnly = true)
    public Contact getContactEntity(UUID tenantId, UUID contactId) {
        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact", contactId));
        if (!contact.getTenant().getId().equals(tenantId)) {
            throw new ResourceNotFoundException("Contact", contactId);
        }
        return contact;
    }

    @Transactional(readOnly = true)
    public ContactDto getContact(UUID tenantId, UUID contactId) {
        return ContactDto.from(getContactEntity(tenantId, contactId));
    }

    @Transactional(readOnly = true)
    public List<ContactDto> listContacts(UUID tenantId) {
        return contactRepository.findAll().stream()
                .filter(contact -> contact.getTenant().getId().equals(tenantId))
                .map(ContactDto::from)
                .toList();
    }
}
