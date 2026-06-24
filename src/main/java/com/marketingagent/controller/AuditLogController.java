package com.marketingagent.controller;

import com.marketingagent.dto.audit.AuditLogDto;
import com.marketingagent.repository.AuditLogRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/tenants/{tenantId}/audit-logs")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    public AuditLogController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    public ResponseEntity<List<AuditLogDto>> getAuditLogs(@PathVariable UUID tenantId) {
        List<AuditLogDto> logs = auditLogRepository.findByTenant_IdOrderByOccurredAtDesc(tenantId)
                .stream()
                .map(AuditLogDto::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(logs);
    }
}
