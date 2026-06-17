package com.marketingagent.dto.agent;

import com.marketingagent.domain.agent.AgentTaskType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;
import java.util.UUID;

public record AgentTaskCreateRequest(
        @NotNull AgentTaskType taskType,
        UUID requestedByUserId,
        @Size(max = 120) String modelName,
        @Size(max = 80) String promptVersion,
        Map<String, Object> inputContext
) {
}
