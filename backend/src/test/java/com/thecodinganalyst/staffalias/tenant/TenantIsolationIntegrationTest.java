package com.thecodinganalyst.staffalias.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.persistence.EntityNotFoundException;

@Testcontainers
@SpringBootTest
class TenantIsolationIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired TenantRepository tenantRepository;
    @Autowired TenantSettingRepository settingRepository;
    @Autowired TenantSettingService settingService;
    @Autowired TenantContext tenantContext;

    private Tenant tenantA;
    private Tenant tenantB;

    @BeforeEach
    void setUp() {
        settingRepository.deleteAll();
        tenantRepository.deleteAll();
        tenantA = tenantRepository.save(new Tenant("TENANT_A", "Tenant A"));
        tenantB = tenantRepository.save(new Tenant("TENANT_B", "Tenant B"));
    }

    @AfterEach
    void clearTenantContext() {
        tenantContext.clear();
    }

    @Test
    void sameBusinessKeyCanExistInDifferentTenants() {
        tenantContext.setTenantId(tenantA.getId());
        TenantSetting a = settingService.create("employee-id-prefix", "A-");

        tenantContext.setTenantId(tenantB.getId());
        TenantSetting b = settingService.create("employee-id-prefix", "B-");

        assertThat(a.getId()).isNotEqualTo(b.getId());
        assertThat(settingRepository.count()).isEqualTo(2);
    }

    @Test
    void tenantCannotReadAnotherTenantsRecord() {
        UUID settingId = createSettingForTenantA();
        tenantContext.setTenantId(tenantB.getId());

        assertThatThrownBy(() -> settingService.get(settingId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void tenantCannotUpdateAnotherTenantsRecord() {
        UUID settingId = createSettingForTenantA();
        tenantContext.setTenantId(tenantB.getId());

        assertThatThrownBy(() -> settingService.update(settingId, "changed"))
                .isInstanceOf(EntityNotFoundException.class);

        tenantContext.setTenantId(tenantA.getId());
        assertThat(settingService.get(settingId).getValue()).isEqualTo("original");
    }

    @Test
    void tenantCannotDeleteAnotherTenantsRecord() {
        UUID settingId = createSettingForTenantA();
        tenantContext.setTenantId(tenantB.getId());

        assertThatThrownBy(() -> settingService.delete(settingId))
                .isInstanceOf(EntityNotFoundException.class);

        tenantContext.setTenantId(tenantA.getId());
        assertThat(settingService.get(settingId).getId()).isEqualTo(settingId);
    }

    private UUID createSettingForTenantA() {
        tenantContext.setTenantId(tenantA.getId());
        return settingService.create("example", "original").getId();
    }
}
