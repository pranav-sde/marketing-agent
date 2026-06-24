package com.marketingagent.controller;

import com.marketingagent.dto.contact.ConsentEventDto;
import com.marketingagent.dto.contact.ConsentRecordRequest;
import com.marketingagent.dto.contact.BulkContactImportRequest;
import com.marketingagent.dto.contact.BulkContactImportResultDto;
import com.marketingagent.dto.contact.ContactCreateRequest;
import com.marketingagent.dto.contact.ContactDto;
import com.marketingagent.service.ConsentService;
import com.marketingagent.service.ContactService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/tenants/{tenantId}/contacts")
public class ContactController {

    private final ContactService contactService;
    private final ConsentService consentService;

    public ContactController(ContactService contactService, ConsentService consentService) {
        this.contactService = contactService;
        this.consentService = consentService;
    }

    @PostMapping
    public ResponseEntity<ContactDto> createContact(
            @PathVariable UUID tenantId,
            @Valid @RequestBody ContactCreateRequest request
    ) {
        ContactDto created = contactService.createContact(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/bulk")
    public ResponseEntity<BulkContactImportResultDto> importContacts(
            @PathVariable UUID tenantId,
            @Valid @RequestBody BulkContactImportRequest request
    ) {
        BulkContactImportResultDto result = contactService.importContacts(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping
    public ResponseEntity<List<ContactDto>> listContacts(@PathVariable UUID tenantId) {
        List<ContactDto> contacts = contactService.listContacts(tenantId);
        return ResponseEntity.ok(contacts);
    }

    @GetMapping("/{contactId}")
    public ResponseEntity<ContactDto> getContact(
            @PathVariable UUID tenantId,
            @PathVariable UUID contactId
    ) {
        ContactDto contact = contactService.getContact(tenantId, contactId);
        return ResponseEntity.ok(contact);
    }

    @PostMapping("/{contactId}/consent-events")
    public ResponseEntity<ConsentEventDto> recordConsent(
            @PathVariable UUID tenantId,
            @PathVariable UUID contactId,
            @Valid @RequestBody ConsentRecordRequest request
    ) {
        ConsentEventDto recorded = consentService.recordConsent(tenantId, contactId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(recorded);
    }

    @GetMapping("/{contactId}/consent-events")
    public ResponseEntity<List<ConsentEventDto>> listConsentEvents(
            @PathVariable UUID tenantId,
            @PathVariable UUID contactId
    ) {
        List<ConsentEventDto> events = consentService.listConsentEvents(tenantId, contactId);
        return ResponseEntity.ok(events);
    }
}
