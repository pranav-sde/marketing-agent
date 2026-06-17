package com.marketingagent.repository;

import com.marketingagent.domain.template.MessageTemplate;
import com.marketingagent.domain.template.TemplateStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageTemplateRepository extends JpaRepository<MessageTemplate, UUID> {
    List<MessageTemplate> findByTenant_IdAndStatus(UUID tenantId, TemplateStatus status);

    Optional<MessageTemplate> findByWhatsAppAccount_IdAndNameAndLanguage(
            UUID whatsAppAccountId,
            String name,
            String language
    );

    Optional<MessageTemplate> findByProviderTemplateId(String providerTemplateId);
}
