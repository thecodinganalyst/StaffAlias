package com.thecodinganalyst.staffalias.tenant;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;

@Service
public class TenantSettingService {

    private final TenantContext tenantContext;
    private final TenantRepository tenantRepository;
    private final TenantSettingRepository settingRepository;

    public TenantSettingService(TenantContext tenantContext, TenantRepository tenantRepository,
            TenantSettingRepository settingRepository) {
        this.tenantContext = tenantContext;
        this.tenantRepository = tenantRepository;
        this.settingRepository = settingRepository;
    }

    @Transactional
    public TenantSetting create(String key, String value) {
        UUID tenantId = tenantContext.requireTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));
        return settingRepository.save(new TenantSetting(tenant, key, value));
    }

    @Transactional(readOnly = true)
    public TenantSetting get(UUID id) {
        return settingRepository.findByIdAndTenant_Id(id, tenantContext.requireTenantId())
                .orElseThrow(() -> new EntityNotFoundException("Tenant setting not found"));
    }

    @Transactional(readOnly = true)
    public List<TenantSetting> list() {
        return settingRepository.findAllByTenant_Id(tenantContext.requireTenantId());
    }

    @Transactional
    public TenantSetting update(UUID id, String value) {
        TenantSetting setting = get(id);
        setting.updateValue(value);
        return setting;
    }

    @Transactional
    public void delete(UUID id) {
        settingRepository.delete(get(id));
    }
}
