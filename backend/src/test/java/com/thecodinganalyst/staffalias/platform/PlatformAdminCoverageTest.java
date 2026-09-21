package com.thecodinganalyst.staffalias.platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.thecodinganalyst.staffalias.security.ApplicationRole;
import com.thecodinganalyst.staffalias.security.ApplicationUser;
import com.thecodinganalyst.staffalias.security.ApplicationUserRepository;
import com.thecodinganalyst.staffalias.security.AccountActivationService;
import com.thecodinganalyst.staffalias.tenant.Tenant;
import com.thecodinganalyst.staffalias.tenant.TenantRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

class PlatformAdminCoverageTest {

    @Test
    void bootstrapCreatesPlatformAdminWithoutTenant() throws Exception {
        ApplicationUserRepository users = Mockito.mock(ApplicationUserRepository.class);
        PasswordEncoder encoder = Mockito.mock(PasswordEncoder.class);
        when(users.findByUsernameIgnoreCase("platform")).thenReturn(Optional.empty());
        when(encoder.encode("secret")).thenReturn("encoded");
        when(users.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        new PlatformAdminBootstrap(users, encoder, "platform", "secret")
                .run(new DefaultApplicationArguments(new String[0]));

        verify(users).save(any(ApplicationUser.class));
    }

    @Test
    void bootstrapIsIdempotentAndRejectsInvalidConfiguration() throws Exception {
        ApplicationUserRepository users = Mockito.mock(ApplicationUserRepository.class);
        PasswordEncoder encoder = Mockito.mock(PasswordEncoder.class);

        new PlatformAdminBootstrap(users, encoder, "", "")
                .run(new DefaultApplicationArguments(new String[0]));
        verify(users, never()).save(any());

        when(users.findByUsernameIgnoreCase("platform"))
                .thenReturn(Optional.of(new ApplicationUser("platform", "existing",
                        ApplicationRole.PLATFORM_ADMIN, null)));
        new PlatformAdminBootstrap(users, encoder, "platform", "new-secret")
                .run(new DefaultApplicationArguments(new String[0]));
        verify(encoder, never()).encode("new-secret");

        assertThatThrownBy(() -> new PlatformAdminBootstrap(users, encoder, "platform", "")
                .run(new DefaultApplicationArguments(new String[0])))
                .isInstanceOf(IllegalStateException.class);

        Tenant tenant = new Tenant("ACME", "Acme");
        when(users.findByUsernameIgnoreCase("occupied"))
                .thenReturn(Optional.of(new ApplicationUser("occupied", "existing",
                        ApplicationRole.TENANT_ADMIN, tenant)));
        assertThatThrownBy(() -> new PlatformAdminBootstrap(users, encoder, "occupied", "secret")
                .run(new DefaultApplicationArguments(new String[0])))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void platformTenantServiceCoversProvisioningConflictsAndMissingTenant() {
        TenantRepository tenants = Mockito.mock(TenantRepository.class);
        ApplicationUserRepository users = Mockito.mock(ApplicationUserRepository.class);
        AccountActivationService activation = Mockito.mock(AccountActivationService.class);
        PlatformTenantAdminService service = new PlatformTenantAdminService(tenants, users, activation);
        Tenant existing = new Tenant("ACME", "Acme");
        UUID existingId = UUID.randomUUID();
        UUID missing = UUID.randomUUID();

        when(tenants.findAll()).thenReturn(List.of(existing));
        assertThat(service.listTenants()).containsExactly(existing);

        when(tenants.findById(existingId)).thenReturn(Optional.of(existing));
        assertThat(service.getTenant(existingId)).isSameAs(existing);
        when(tenants.findById(missing)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getTenant(missing)).isInstanceOf(EntityNotFoundException.class);

        when(tenants.findByCode("ACME")).thenReturn(Optional.of(existing));
        assertThatThrownBy(() -> service.provisionTenant("ACME", "Other", "admin-a@example.com"))
                .isInstanceOf(ResponseStatusException.class);

        when(tenants.findByCode("NEW")).thenReturn(Optional.empty());
        when(users.findByUsernameIgnoreCase("occupied")).thenReturn(Optional.of(
                new ApplicationUser("occupied", "hash", ApplicationRole.TENANT_ADMIN, existing)));
        assertThatThrownBy(() -> service.provisionTenant("NEW", "New", "occupied"))
                .isInstanceOf(ResponseStatusException.class);

        when(users.findByUsernameIgnoreCase("new-admin@example.com")).thenReturn(Optional.empty());
        when(tenants.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(users.save(any(ApplicationUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(activation.issue(any(ApplicationUser.class), org.mockito.ArgumentMatchers.eq("New Tenant"))).thenReturn(true);

        PlatformTenantAdminService.TenantProvisioningResult result =
                service.provisionTenant("NEW", "New Tenant", "new-admin@example.com");
        assertThat(result.tenant().getCode()).isEqualTo("NEW");
        assertThat(result.tenantAdmin().getRole()).isEqualTo(ApplicationRole.TENANT_ADMIN);
        assertThat(result.tenantAdmin().getTenant()).isSameAs(result.tenant());
        assertThat(result.tenantAdmin().getPasswordHash()).isNull();
        assertThat(result.tenantAdmin().getEmail()).isEqualTo("new-admin@example.com");
        assertThat(result.tenantAdmin().isEnabled()).isFalse();
        assertThat(result.activationEmailSent()).isTrue();

        when(tenants.findById(existingId)).thenReturn(Optional.of(existing));
        when(tenants.save(existing)).thenReturn(existing);
        assertThat(service.updateTenant(existingId, "Renamed").getName()).isEqualTo("Renamed");

    }
}
