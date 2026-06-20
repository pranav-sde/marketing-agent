package com.marketingagent.service;

import com.marketingagent.domain.tenant.Tenant;
import com.marketingagent.dto.tenant.TenantCreateRequest;
import com.marketingagent.dto.tenant.TenantDto;
import com.marketingagent.dto.tenant.TenantSettingsDto;
import com.marketingagent.exception.ConflictException;
import com.marketingagent.exception.ResourceNotFoundException;
import com.marketingagent.repository.TenantRepository;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class TenantService {

    private final TenantRepository tenantRepository;

    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Transactional
    public TenantDto createTenant(@Valid TenantCreateRequest request) {
        tenantRepository.findBySlug(request.slug()).ifPresent(existing -> {
            throw new ConflictException("Tenant slug already exists: " + request.slug());
        });
        Tenant tenant = new Tenant(request.name(), request.slug(), request.timezone(), request.defaultLocale());
        return TenantDto.from(tenantRepository.save(tenant));
    }

    @Transactional(readOnly = true)
    public Tenant getTenantEntity(UUID tenantId) {
        return tenantRepository.findById(tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));
    }

    @Transactional(readOnly = true)
    public TenantDto getTenant(UUID tenantId) {
        return TenantDto.from(getTenantEntity(tenantId));
    }

    @Transactional(readOnly = true)
    public List<TenantDto> listTenants() {
        return tenantRepository.findAll().stream().map(TenantDto::from).toList();
    }

    @Transactional(readOnly = true)
    public TenantSettingsDto getSettings(UUID tenantId) {
        Tenant tenant = getTenantEntity(tenantId);
        String token = tenant.getWhatsappAccessToken();
        String maskedToken = null;
        if (token != null) {
            if (token.length() > 8) {
                maskedToken = token.substring(0, 4) + "********" + token.substring(token.length() - 4);
            } else {
                maskedToken = "********";
            }
        }
        return new TenantSettingsDto(maskedToken, tenant.getWhatsappPhoneNumberId());
    }

    @Transactional
    public TenantSettingsDto saveSettings(UUID tenantId, @Valid TenantSettingsDto settings) {
        Tenant tenant = getTenantEntity(tenantId);
        
        String token = settings.whatsappAccessToken();
        if (token != null) {
            if (token.trim().isEmpty()) {
                tenant.setWhatsappAccessToken(null);
            } else if (!token.contains("*")) {
                tenant.setWhatsappAccessToken(token.trim());
            }
        }
        
        String phoneId = settings.whatsappPhoneNumberId();
        tenant.setWhatsappPhoneNumberId(phoneId != null ? phoneId.trim() : null);
        
        tenantRepository.save(tenant);
        return getSettings(tenantId);
    }
}
