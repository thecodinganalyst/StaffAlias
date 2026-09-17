package com.thecodinganalyst.staffalias.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityNotFoundException;

class TenantServicesCoverageTest {

    @Test
    void tenantDomainAndThreadLocalContextAreCovered() {
        Tenant tenant = new Tenant("TENANT", "Tenant Name");
        assertThat(tenant.getId()).isNull();
        assertThat(tenant.getCode()).isEqualTo("TENANT");
        assertThat(tenant.getName()).isEqualTo("Tenant Name");

        TenantSetting setting = new TenantSetting(tenant, "timezone", "UTC");
        assertThat(setting.getId()).isNull();
        assertThat(setting.getTenant()).isSameAs(tenant);
        assertThat(setting.getKey()).isEqualTo("timezone");
        assertThat(setting.getValue()).isEqualTo("UTC");
        setting.updateValue("Asia/Singapore");
        assertThat(setting.getValue()).isEqualTo("Asia/Singapore");

        ThreadLocalTenantContext context = new ThreadLocalTenantContext();
        assertThatThrownBy(context::requireTenantId).isInstanceOf(IllegalStateException.class);
        UUID tenantId = UUID.randomUUID();
        context.setTenantId(tenantId);
        assertThat(context.requireTenantId()).isEqualTo(tenantId);
        context.clear();
        assertThatThrownBy(context::requireTenantId).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void tenantResolutionServiceCoversSuccessMissingAndClear() {
        TenantRepository repository = mock(TenantRepository.class);
        TenantContext context = mock(TenantContext.class);
        TenantResolutionService service = new TenantResolutionService(repository, context);
        Tenant tenant = mock(Tenant.class);
        UUID tenantId = UUID.randomUUID();
        when(tenant.getId()).thenReturn(tenantId);
        when(repository.findByCode("TENANT")).thenReturn(Optional.of(tenant));

        service.establishByCode("TENANT");
        verify(context).setTenantId(tenantId);
        service.clear();
        verify(context).clear();

        when(repository.findByCode("MISSING")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.establishByCode("MISSING"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void tenantSettingServiceCoversCrudAndMissingRecords() {
        TenantContext context = mock(TenantContext.class);
        TenantRepository tenantRepository = mock(TenantRepository.class);
        TenantSettingRepository settingRepository = mock(TenantSettingRepository.class);
        TenantSettingService service = new TenantSettingService(context, tenantRepository, settingRepository);
        UUID tenantId = UUID.randomUUID();
        UUID settingId = UUID.randomUUID();
        Tenant tenant = new Tenant("TENANT", "Tenant");
        TenantSetting setting = new TenantSetting(tenant, "key", "old");

        when(context.requireTenantId()).thenReturn(tenantId);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(settingRepository.save(org.mockito.ArgumentMatchers.any(TenantSetting.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TenantSetting created = service.create("key", "value");
        assertThat(created.getTenant()).isSameAs(tenant);
        assertThat(created.getValue()).isEqualTo("value");

        when(settingRepository.findByIdAndTenant_Id(settingId, tenantId)).thenReturn(Optional.of(setting));
        assertThat(service.get(settingId)).isSameAs(setting);
        when(settingRepository.findAllByTenant_Id(tenantId)).thenReturn(List.of(setting));
        assertThat(service.list()).containsExactly(setting);
        assertThat(service.update(settingId, "new").getValue()).isEqualTo("new");
        service.delete(settingId);
        verify(settingRepository).delete(setting);

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create("missing", "value"))
                .isInstanceOf(EntityNotFoundException.class);
        when(settingRepository.findByIdAndTenant_Id(settingId, tenantId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(settingId)).isInstanceOf(EntityNotFoundException.class);
    }
}
