package com.marketingagent.domain.agent;

import com.marketingagent.domain.common.BaseEntity;
import com.marketingagent.domain.tenant.Tenant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
@Table(
        name = "agent_policy_checks",
        indexes = {
                @Index(name = "idx_agent_policy_checks_tenant", columnList = "tenant_id"),
                @Index(name = "idx_agent_policy_checks_task", columnList = "agent_task_id")
        }
)
public class AgentPolicyCheck extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_task_id", nullable = false)
    private AgentTask agentTask;

    @NotBlank
    @Column(name = "policy_name", nullable = false, length = 120)
    private String policyName;

    @Column(name = "passed", nullable = false)
    private boolean passed;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details", columnDefinition = "jsonb")
    private Map<String, Object> details = new LinkedHashMap<>();

    protected AgentPolicyCheck() {
    }

    public AgentPolicyCheck(Tenant tenant, AgentTask agentTask, String policyName, boolean passed) {
        this.tenant = tenant;
        this.agentTask = agentTask;
        this.policyName = policyName;
        this.passed = passed;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public AgentTask getAgentTask() {
        return agentTask;
    }

    public String getPolicyName() {
        return policyName;
    }

    public boolean isPassed() {
        return passed;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = new LinkedHashMap<>(details);
    }
}
