package com.marketingagent.controller;

import com.marketingagent.dto.tenant.TenantSettingsDto;
import com.marketingagent.service.TenantService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/tenants/{tenantId}/settings")
public class TenantSettingsController {

    private final TenantService tenantService;

    public TenantSettingsController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping
    public ResponseEntity<TenantSettingsDto> getSettings(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(tenantService.getSettings(tenantId));
    }

    @PutMapping
    public ResponseEntity<TenantSettingsDto> saveSettings(
            @PathVariable UUID tenantId,
            @Valid @RequestBody TenantSettingsDto settings) {
        return ResponseEntity.ok(tenantService.saveSettings(tenantId, settings));
    }
}
