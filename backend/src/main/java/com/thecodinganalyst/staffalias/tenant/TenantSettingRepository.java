package com.thecodinganalyst.staffalias.tenant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantSettingRepository extends JpaRepository<TenantSetting, UUID> {
    Optional<TenantSetting> findByIdAndTenant_Id(UUID id, UUID tenantId);
    Optional<TenantSetting> findByTenant_IdAndKey(UUID tenantId, String key);
    List<TenantSetting> findAllByTenant_Id(UUID tenantId);
}
