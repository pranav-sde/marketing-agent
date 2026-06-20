package com.marketingagent.repository;

import com.marketingagent.domain.contact.Contact;
import com.marketingagent.domain.contact.ContactStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactRepository extends JpaRepository<Contact, UUID> {
    Optional<Contact> findByTenant_IdAndPhoneE164(UUID tenantId, String phoneE164);

    Optional<Contact> findByTenant_IdAndPhoneHash(UUID tenantId, String phoneHash);

    List<Contact> findByTenant_IdAndStatus(UUID tenantId, ContactStatus status);

    org.springframework.data.domain.Page<Contact> findByTenant_IdAndStatus(UUID tenantId, ContactStatus status, org.springframework.data.domain.Pageable pageable);
}
