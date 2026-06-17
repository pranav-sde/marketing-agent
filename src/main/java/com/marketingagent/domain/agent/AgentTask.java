package com.marketingagent.domain.agent;

import com.marketingagent.domain.common.BaseEntity;
import com.marketingagent.domain.tenant.Tenant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
        name = "agent_tasks",
        indexes = {
                @Index(name = "idx_agent_tasks_tenant_status", columnList = "tenant_id,status"),
                @Index(name = "idx_agent_tasks_type", columnList = "task_type")
        }
)
public class AgentTask extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false, length = 48)
    private AgentTaskType taskType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private AgentTaskStatus status = AgentTaskStatus.REQUESTED;

    @Column(name = "requested_by_user_id")
    private java.util.UUID requestedByUserId;

    @Column(name = "model_name", length = 120)
    private String modelName;

    @Column(name = "prompt_version", length = 80)
    private String promptVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_context", columnDefinition = "jsonb")
    private Map<String, Object> inputContext = new LinkedHashMap<>();

    @Column(name = "completed_at")
    private Instant completedAt;

    protected AgentTask() {
    }

    public AgentTask(Tenant tenant, AgentTaskType taskType) {
        this.tenant = tenant;
        this.taskType = taskType;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public AgentTaskType getTaskType() {
        return taskType;
    }

    public AgentTaskStatus getStatus() {
        return status;
    }

    public void setStatus(AgentTaskStatus status) {
        this.status = status;
    }

    public java.util.UUID getRequestedByUserId() {
        return requestedByUserId;
    }

    public void setRequestedByUserId(java.util.UUID requestedByUserId) {
        this.requestedByUserId = requestedByUserId;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public void setPromptVersion(String promptVersion) {
        this.promptVersion = promptVersion;
    }

    public Map<String, Object> getInputContext() {
        return inputContext;
    }

    public void setInputContext(Map<String, Object> inputContext) {
        this.inputContext = new LinkedHashMap<>(inputContext);
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
