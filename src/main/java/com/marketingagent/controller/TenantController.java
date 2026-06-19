package com.marketingagent.controller;

import com.marketingagent.dto.tenant.TenantCreateRequest;
import com.marketingagent.dto.tenant.TenantDto;
import com.marketingagent.service.TenantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/tenants")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @PostMapping
    public ResponseEntity<TenantDto> createTenant(@Valid @RequestBody TenantCreateRequest request) {
        TenantDto created = tenantService.createTenant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{tenantId}")
    public ResponseEntity<TenantDto> getTenant(@PathVariable UUID tenantId) {
        TenantDto tenant = tenantService.getTenant(tenantId);
        return ResponseEntity.ok(tenant);
    }

    @GetMapping
    public ResponseEntity<List<TenantDto>> listTenants() {
        List<TenantDto> tenants = tenantService.listTenants();
        return ResponseEntity.ok(tenants);
    }
}
