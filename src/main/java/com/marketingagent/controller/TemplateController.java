package com.marketingagent.controller;

import com.marketingagent.dto.template.MessageTemplateDto;
import com.marketingagent.dto.template.TemplateCreateRequest;
import com.marketingagent.service.TemplateService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/tenants/{tenantId}/templates")
public class TemplateController {

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @PostMapping
    public ResponseEntity<MessageTemplateDto> createTemplate(
            @PathVariable UUID tenantId,
            @Valid @RequestBody TemplateCreateRequest request
    ) {
        MessageTemplateDto created = templateService.createTemplate(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<MessageTemplateDto>> listTemplates(@PathVariable UUID tenantId) {
        List<MessageTemplateDto> templates = templateService.listTemplates(tenantId);
        return ResponseEntity.ok(templates);
    }
}
