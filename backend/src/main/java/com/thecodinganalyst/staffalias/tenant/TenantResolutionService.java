package com.thecodinganalyst.staffalias.tenant;

import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;

@Service
public class TenantResolutionService {

    private final TenantRepository tenantRepository;
    private final TenantContext tenantContext;

    public TenantResolutionService(TenantRepository tenantRepository, TenantContext tenantContext) {
        this.tenantRepository = tenantRepository;
        this.tenantContext = tenantContext;
    }

    public void establishByCode(String tenantCode) {
        Tenant tenant = tenantRepository.findByCode(tenantCode)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));
        tenantContext.setTenantId(tenant.getId());
    }

    public void clear() {
        tenantContext.clear();
    }
}
