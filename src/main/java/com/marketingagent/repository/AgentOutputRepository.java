package com.marketingagent.repository;

import com.marketingagent.domain.agent.AgentOutput;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentOutputRepository extends JpaRepository<AgentOutput, UUID> {
    List<AgentOutput> findByTenant_IdAndAgentTask_Id(UUID tenantId, UUID agentTaskId);
}
