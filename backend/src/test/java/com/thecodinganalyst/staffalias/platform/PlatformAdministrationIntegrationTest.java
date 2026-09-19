package com.thecodinganalyst.staffalias.platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.thecodinganalyst.staffalias.security.ApplicationRole;
import com.thecodinganalyst.staffalias.security.ApplicationUser;
import com.thecodinganalyst.staffalias.security.ApplicationUserRepository;
import com.thecodinganalyst.staffalias.tenant.Tenant;
import com.thecodinganalyst.staffalias.tenant.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class PlatformAdministrationIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired MockMvc mockMvc;
    @Autowired ApplicationUserRepository userRepository;
    @Autowired TenantRepository tenantRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private Tenant tenant;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        tenantRepository.deleteAll();
        tenant = tenantRepository.save(new Tenant("EXISTING", "Existing Tenant"));
        userRepository.save(new ApplicationUser("platform", passwordEncoder.encode("platform-pass"),
                ApplicationRole.PLATFORM_ADMIN, null));
        userRepository.save(new ApplicationUser("tenant-admin", passwordEncoder.encode("tenant-pass"),
                ApplicationRole.TENANT_ADMIN, tenant));
    }

    @Test
    void platformAdminCanListViewAndProvisionTenantWithInitialAdmin() throws Exception {
        mockMvc.perform(get("/api/platform/tenants").with(httpBasic("platform", "platform-pass")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("EXISTING"));

        mockMvc.perform(get("/api/platform/tenants/{id}", tenant.getId())
                        .with(httpBasic("platform", "platform-pass")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Existing Tenant"));

        mockMvc.perform(post("/api/platform/tenants")
                        .with(httpBasic("platform", "platform-pass"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"NEWCO","name":"New Company","adminEmail":"new-admin@example.com"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/platform/tenants/")))
                .andExpect(jsonPath("$.tenant.code").value("NEWCO"))
                .andExpect(jsonPath("$.tenant.name").value("New Company"))
                .andExpect(jsonPath("$.tenantAdmin.username").value("new-admin@example.com"))
                .andExpect(jsonPath("$.tenantAdmin.role").value("TENANT_ADMIN"))
                .andExpect(jsonPath("$.tenantAdmin.password").doesNotExist())
                .andExpect(jsonPath("$.tenantAdmin.passwordHash").doesNotExist());

        Tenant createdTenant = tenantRepository.findByCode("NEWCO").orElseThrow();
        ApplicationUser createdAdmin = userRepository.findByUsernameIgnoreCase("new-admin@example.com").orElseThrow();
        assertThat(createdAdmin.getTenant().getId()).isEqualTo(createdTenant.getId());
        assertThat(createdAdmin.getPasswordHash()).isNull();
        assertThat(createdAdmin.isEnabled()).isFalse();
    }

    @Test
    void tenantAdminAndAnonymousUsersCannotProvisionTenants() throws Exception {
        String body = """
                {"code":"BLOCKED","name":"Blocked","adminEmail":"blocked-admin@example.com"}
                """;
        mockMvc.perform(post("/api/platform/tenants")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/platform/tenants")
                        .with(httpBasic("tenant-admin", "tenant-pass"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        assertThat(tenantRepository.findByCode("BLOCKED")).isEmpty();
    }

    @Test
    void duplicateTenantCodeAndUsernameReturnConflictWithoutPartialProvisioning() throws Exception {
        mockMvc.perform(post("/api/platform/tenants")
                        .with(httpBasic("platform", "platform-pass"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"EXISTING","name":"Duplicate","adminEmail":"other@example.com"}
                                """))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/platform/tenants")
                        .with(httpBasic("platform", "platform-pass"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"ROLLBACK","name":"Rollback Tenant","adminEmail":"tenant-admin"}
                                """))
                .andExpect(status().isConflict());

        assertThat(tenantRepository.findByCode("ROLLBACK")).isEmpty();
        assertThat(userRepository.findByUsernameIgnoreCase("other@example.com")).isEmpty();
    }

    @Test
    void invalidProvisioningRequestIsRejected() throws Exception {
        mockMvc.perform(post("/api/platform/tenants")
                        .with(httpBasic("platform", "platform-pass"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"","name":"","adminEmail":"not-an-email"}
                                """))
                .andExpect(status().isBadRequest());
    }
}
