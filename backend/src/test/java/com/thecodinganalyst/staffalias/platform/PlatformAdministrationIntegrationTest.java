package com.thecodinganalyst.staffalias.platform;

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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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
    void platformAdminCanListViewAndCreateTenants() throws Exception {
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
                        .content("{\"code\":\"NEWCO\",\"name\":\"New Company\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/platform/tenants/")))
                .andExpect(jsonPath("$.code").value("NEWCO"))
                .andExpect(jsonPath("$.name").value("New Company"));
    }

    @Test
    void tenantAdminAndAnonymousUsersCannotAccessPlatformAdministration() throws Exception {
        mockMvc.perform(get("/api/platform/tenants"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/platform/tenants").with(httpBasic("tenant-admin", "tenant-pass")))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidTenantCreationIsRejected() throws Exception {
        mockMvc.perform(post("/api/platform/tenants")
                        .with(httpBasic("platform", "platform-pass"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"\",\"name\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
