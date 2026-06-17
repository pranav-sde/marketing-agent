package com.marketingagent.repository;

import com.marketingagent.domain.contact.ContactAttribute;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactAttributeRepository extends JpaRepository<ContactAttribute, UUID> {
    List<ContactAttribute> findByTenant_IdAndContact_Id(UUID tenantId, UUID contactId);

    Optional<ContactAttribute> findByContact_IdAndKey(UUID contactId, String key);
}
