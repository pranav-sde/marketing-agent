package com.marketingagent.repository;

import com.marketingagent.domain.audit.DataExport;
import com.marketingagent.domain.audit.DataExportStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataExportRepository extends JpaRepository<DataExport, UUID> {
    List<DataExport> findByTenant_IdAndStatus(UUID tenantId, DataExportStatus status);

    List<DataExport> findByTenant_IdAndRequestedByUserId(UUID tenantId, UUID requestedByUserId);
}
