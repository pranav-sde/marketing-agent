package com.marketingagent.controller;

import com.marketingagent.model.Tenant;
import com.marketingagent.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/v1/tenants")
public class TenantController {

    @Autowired
    private TenantRepository tenantRepository;

    public record TenantRequest(String name, String slug, String timezone, String defaultLocale) {}
    public record TenantSettingsRequest(String whatsappAccessToken, String whatsappPhoneNumberId) {}

    @GetMapping
    public List<Tenant> getTenants() {
        return tenantRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<?> createTenant(@RequestBody TenantRequest request) {
        if (request.slug() == null || request.slug().isEmpty()) {
            return ResponseEntity.badRequest().body("Tenant slug is required");
        }
        Optional<Tenant> existing = tenantRepository.findBySlug(request.slug());
        if (existing.isPresent()) {
            return ResponseEntity.ok(existing.get()); // Return existing to simplify UI flow
        }
        Tenant tenant = new Tenant(request.name(), request.slug(), request.timezone(), request.defaultLocale());
        Tenant saved = tenantRepository.save(tenant);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/{id}/settings")
    public ResponseEntity<?> getSettings(@PathVariable UUID id) {
        return tenantRepository.findById(id)
                .map(tenant -> ResponseEntity.ok(new TenantSettingsRequest(
                        tenant.getWhatsappAccessToken(),
                        tenant.getWhatsappPhoneNumberId()
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/settings")
    public ResponseEntity<?> updateSettings(@PathVariable UUID id, @RequestBody TenantSettingsRequest settings) {
        return tenantRepository.findById(id)
                .map(tenant -> {
                    tenant.setWhatsappAccessToken(settings.whatsappAccessToken());
                    tenant.setWhatsappPhoneNumberId(settings.whatsappPhoneNumberId());
                    Tenant updated = tenantRepository.save(tenant);
                    return ResponseEntity.ok(new TenantSettingsRequest(
                            updated.getWhatsappAccessToken(),
                            updated.getWhatsappPhoneNumberId()
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
