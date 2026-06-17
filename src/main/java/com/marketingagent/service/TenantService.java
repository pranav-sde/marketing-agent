package com.marketingagent.service;

import com.marketingagent.domain.tenant.Tenant;
import com.marketingagent.dto.tenant.TenantCreateRequest;
import com.marketingagent.dto.tenant.TenantDto;
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
}
