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
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
@Table(
        name = "agent_outputs",
        indexes = {
                @Index(name = "idx_agent_outputs_tenant", columnList = "tenant_id"),
                @Index(name = "idx_agent_outputs_task", columnList = "agent_task_id")
        }
)
public class AgentOutput extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_task_id", nullable = false)
    private AgentTask agentTask;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> content = new LinkedHashMap<>();

    @Column(name = "approved_by_user_id")
    private java.util.UUID approvedByUserId;

    @Column(name = "approved_at")
    private Instant approvedAt;

    protected AgentOutput() {
    }

    public AgentOutput(Tenant tenant, AgentTask agentTask, Map<String, Object> content) {
        this.tenant = tenant;
        this.agentTask = agentTask;
        this.content = new LinkedHashMap<>(content);
    }

    public Tenant getTenant() {
        return tenant;
    }

    public AgentTask getAgentTask() {
        return agentTask;
    }

    public Map<String, Object> getContent() {
        return content;
    }

    public void setContent(Map<String, Object> content) {
        this.content = new LinkedHashMap<>(content);
    }

    public java.util.UUID getApprovedByUserId() {
        return approvedByUserId;
    }

    public void setApprovedByUserId(java.util.UUID approvedByUserId) {
        this.approvedByUserId = approvedByUserId;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Instant approvedAt) {
        this.approvedAt = approvedAt;
    }
}
