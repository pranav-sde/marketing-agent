package com.marketingagent.repository;

import com.marketingagent.domain.template.TemplateVersion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplateVersionRepository extends JpaRepository<TemplateVersion, UUID> {
    List<TemplateVersion> findByTenant_IdAndMessageTemplate_IdOrderByVersionNumberDesc(
            UUID tenantId,
            UUID messageTemplateId
    );

    Optional<TemplateVersion> findByMessageTemplate_IdAndVersionNumber(UUID messageTemplateId, int versionNumber);
}
