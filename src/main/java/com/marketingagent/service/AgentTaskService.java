package com.marketingagent.service;

import com.marketingagent.domain.agent.AgentTask;
import com.marketingagent.domain.tenant.Tenant;
import com.marketingagent.dto.agent.AgentTaskCreateRequest;
import com.marketingagent.dto.agent.AgentTaskDto;
import com.marketingagent.exception.ResourceNotFoundException;
import com.marketingagent.repository.AgentTaskRepository;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class AgentTaskService {

    private final TenantService tenantService;
    private final AgentTaskRepository agentTaskRepository;

    public AgentTaskService(TenantService tenantService, AgentTaskRepository agentTaskRepository) {
        this.tenantService = tenantService;
        this.agentTaskRepository = agentTaskRepository;
    }

    @Transactional
    public AgentTaskDto createTask(UUID tenantId, @Valid AgentTaskCreateRequest request) {
        Tenant tenant = tenantService.getTenantEntity(tenantId);
        AgentTask task = new AgentTask(tenant, request.taskType());
        task.setRequestedByUserId(request.requestedByUserId());
        task.setModelName(request.modelName());
        task.setPromptVersion(request.promptVersion());
        task.setInputContext(request.inputContext() == null ? Map.of() : request.inputContext());
        return AgentTaskDto.from(agentTaskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public AgentTaskDto getTask(UUID tenantId, UUID taskId) {
        AgentTask task = agentTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("AgentTask", taskId));
        if (!task.getTenant().getId().equals(tenantId)) {
            throw new ResourceNotFoundException("AgentTask", taskId);
        }
        return AgentTaskDto.from(task);
    }

    @Transactional(readOnly = true)
    public List<AgentTaskDto> listTasks(UUID tenantId) {
        return agentTaskRepository.findAll().stream()
                .filter(task -> task.getTenant().getId().equals(tenantId))
                .map(AgentTaskDto::from)
                .toList();
    }
}
