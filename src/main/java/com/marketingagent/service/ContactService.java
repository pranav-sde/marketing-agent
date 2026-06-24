package com.marketingagent.service;

import com.marketingagent.domain.contact.Contact;
import com.marketingagent.domain.tenant.Tenant;
import com.marketingagent.dto.contact.BulkContactImportRequest;
import com.marketingagent.dto.contact.BulkContactImportResultDto;
import com.marketingagent.dto.contact.ContactCreateRequest;
import com.marketingagent.dto.contact.ContactDto;
import com.marketingagent.exception.ConflictException;
import com.marketingagent.exception.ResourceNotFoundException;
import com.marketingagent.repository.ContactRepository;
import jakarta.validation.Valid;
import java.util.ArrayList;
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
        Tenant tenant = tenantService.getTenantEntity(tenantId);
        return ContactDto.from(createContactEntity(tenant, request, true));
    }

    @Transactional
    public BulkContactImportResultDto importContacts(UUID tenantId, @Valid BulkContactImportRequest request) {
        Tenant tenant = tenantService.getTenantEntity(tenantId);
        List<ContactDto> createdContacts = new ArrayList<>();
        List<String> skippedPhoneNumbers = new ArrayList<>();

        for (ContactCreateRequest contactRequest : request.contacts()) {
            try {
                createdContacts.add(ContactDto.from(createContactEntity(tenant, contactRequest, true)));
            } catch (ConflictException exception) {
                skippedPhoneNumbers.add(contactRequest.phoneE164());
            }
        }

        return new BulkContactImportResultDto(
                createdContacts.size(),
                skippedPhoneNumbers.size(),
                createdContacts,
                skippedPhoneNumbers
        );
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
        return contactRepository.findByTenant_Id(tenantId).stream()
                .map(ContactDto::from)
                .toList();
    }

    private Contact createContactEntity(Tenant tenant, ContactCreateRequest request, boolean failOnDuplicate) {
        if (contactRepository.findByTenant_IdAndPhoneE164(tenant.getId(), request.phoneE164()).isPresent()) {
            if (failOnDuplicate) {
                throw new ConflictException("Contact already exists for phone number");
            }
            throw new ConflictException("Duplicate contact");
        }

        Contact contact = new Contact(tenant, request.phoneE164(), phoneHashingService.hashPhone(request.phoneE164()));
        contact.setFirstName(request.firstName());
        contact.setLastName(request.lastName());
        contact.setEmail(request.email());
        return contactRepository.save(contact);
    }
}
