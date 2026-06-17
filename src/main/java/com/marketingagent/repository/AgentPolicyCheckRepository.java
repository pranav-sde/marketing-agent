package com.marketingagent.repository;

import com.marketingagent.domain.agent.AgentPolicyCheck;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentPolicyCheckRepository extends JpaRepository<AgentPolicyCheck, UUID> {
    List<AgentPolicyCheck> findByTenant_IdAndAgentTask_Id(UUID tenantId, UUID agentTaskId);
}
