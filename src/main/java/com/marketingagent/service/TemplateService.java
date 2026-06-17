package com.marketingagent.service;

import com.marketingagent.domain.integration.WhatsAppAccount;
import com.marketingagent.domain.template.MessageTemplate;
import com.marketingagent.domain.template.TemplateVersion;
import com.marketingagent.domain.tenant.Tenant;
import com.marketingagent.dto.template.MessageTemplateDto;
import com.marketingagent.dto.template.TemplateCreateRequest;
import com.marketingagent.exception.ResourceNotFoundException;
import com.marketingagent.repository.MessageTemplateRepository;
import com.marketingagent.repository.TemplateVersionRepository;
import com.marketingagent.repository.WhatsAppAccountRepository;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class TemplateService {

    private final TenantService tenantService;
    private final WhatsAppAccountRepository whatsAppAccountRepository;
    private final MessageTemplateRepository messageTemplateRepository;
    private final TemplateVersionRepository templateVersionRepository;

    public TemplateService(
            TenantService tenantService,
            WhatsAppAccountRepository whatsAppAccountRepository,
            MessageTemplateRepository messageTemplateRepository,
            TemplateVersionRepository templateVersionRepository
    ) {
        this.tenantService = tenantService;
        this.whatsAppAccountRepository = whatsAppAccountRepository;
        this.messageTemplateRepository = messageTemplateRepository;
        this.templateVersionRepository = templateVersionRepository;
    }

    @Transactional
    public MessageTemplateDto createTemplate(UUID tenantId, @Valid TemplateCreateRequest request) {
        Tenant tenant = tenantService.getTenantEntity(tenantId);
        WhatsAppAccount account = whatsAppAccountRepository.findById(request.whatsAppAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("WhatsAppAccount", request.whatsAppAccountId()));
        if (!account.getTenant().getId().equals(tenantId)) {
            throw new ResourceNotFoundException("WhatsAppAccount", request.whatsAppAccountId());
        }

        MessageTemplate template = new MessageTemplate(
                tenant,
                account,
                request.name(),
                request.language(),
                request.category()
        );
        MessageTemplate savedTemplate = messageTemplateRepository.save(template);
        TemplateVersion version = new TemplateVersion(tenant, savedTemplate, 1, request.components());
        version.setVariableExamples(request.variableExamples() == null ? Map.of() : request.variableExamples());
        templateVersionRepository.save(version);
        return MessageTemplateDto.from(savedTemplate);
    }

    @Transactional(readOnly = true)
    public MessageTemplate getTemplateEntity(UUID tenantId, UUID templateId) {
        MessageTemplate template = messageTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("MessageTemplate", templateId));
        if (!template.getTenant().getId().equals(tenantId)) {
            throw new ResourceNotFoundException("MessageTemplate", templateId);
        }
        return template;
    }

    @Transactional(readOnly = true)
    public List<MessageTemplateDto> listTemplates(UUID tenantId) {
        return messageTemplateRepository.findAll().stream()
                .filter(template -> template.getTenant().getId().equals(tenantId))
                .map(MessageTemplateDto::from)
                .toList();
    }
}
