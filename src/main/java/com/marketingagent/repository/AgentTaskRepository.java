package com.marketingagent.repository;

import com.marketingagent.domain.agent.AgentTask;
import com.marketingagent.domain.agent.AgentTaskStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentTaskRepository extends JpaRepository<AgentTask, UUID> {
    List<AgentTask> findByTenant_IdAndStatus(UUID tenantId, AgentTaskStatus status);

    List<AgentTask> findByTenant_IdAndRequestedByUserId(UUID tenantId, UUID requestedByUserId);
}
