package com.marketingagent.dto.agent;

import com.marketingagent.domain.agent.AgentTask;
import com.marketingagent.domain.agent.AgentTaskStatus;
import com.marketingagent.domain.agent.AgentTaskType;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AgentTaskDto(
        UUID id,
        UUID tenantId,
        AgentTaskType taskType,
        AgentTaskStatus status,
        UUID requestedByUserId,
        String modelName,
        String promptVersion,
        Map<String, Object> inputContext,
        Instant completedAt
) {
    public static AgentTaskDto from(AgentTask task) {
        return new AgentTaskDto(
                task.getId(),
                task.getTenant().getId(),
                task.getTaskType(),
                task.getStatus(),
                task.getRequestedByUserId(),
                task.getModelName(),
                task.getPromptVersion(),
                task.getInputContext(),
                task.getCompletedAt()
        );
    }
}
