package com.thecodinganalyst.staffalias.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import com.thecodinganalyst.staffalias.tenant.Tenant;
import com.thecodinganalyst.staffalias.tenant.TenantContext;
import com.thecodinganalyst.staffalias.tenant.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@Import(SecurityIntegrationTest.TenantProbeController.class)
class SecurityIntegrationTest {

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
        tenant = tenantRepository.save(new Tenant("ACME", "Acme"));
        userRepository.save(new ApplicationUser("platform", passwordEncoder.encode("platform-pass"),
                ApplicationRole.PLATFORM_ADMIN, null));
        userRepository.save(new ApplicationUser("tenant-admin", passwordEncoder.encode("tenant-pass"),
                ApplicationRole.TENANT_ADMIN, tenant));
    }

    @Test
    void anonymousUserIsRejected() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidPasswordIsRejected() throws Exception {
        mockMvc.perform(get("/api/auth/me").with(httpBasic("platform", "wrong")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sessionLoginMeAndLogoutWorkWithoutPersistingCredentialsClientSide() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"platform\",\"password\":\"platform-pass\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("platform"))
                .andExpect(jsonPath("$.role").value("PLATFORM_ADMIN"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertThat(session).isNotNull();

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("platform"));

        mockMvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isNoContent());
    }

    @Test
    void invalidSessionLoginIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"platform\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void platformAdminAuthenticatesWithoutTenantContext() throws Exception {
        mockMvc.perform(get("/api/auth/me").with(httpBasic("PLATFORM", "platform-pass")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("platform"))
                .andExpect(jsonPath("$.role").value("PLATFORM_ADMIN"))
                .andExpect(jsonPath("$.tenantId").doesNotExist());
    }

    @Test
    void tenantAdminAuthenticatesAndTenantContextComesFromPrincipal() throws Exception {
        mockMvc.perform(get("/api/tenant/context")
                        .header("X-Tenant-Id", "00000000-0000-0000-0000-000000000000")
                        .with(httpBasic("tenant-admin", "tenant-pass")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value(tenant.getId().toString()));
    }

    @Test
    void tenantAdminCannotAccessPlatformEndpoints() throws Exception {
        mockMvc.perform(get("/api/platform/not-present").with(httpBasic("tenant-admin", "tenant-pass")))
                .andExpect(status().isForbidden());
    }

    @Test
    void platformAdminCannotAccessTenantEndpoints() throws Exception {
        mockMvc.perform(get("/api/tenant/context").with(httpBasic("platform", "platform-pass")))
                .andExpect(status().isForbidden());
    }

    @Test
    void modelRejectsInvalidRoleTenantCombinations() {
        assertThatThrownBy(() -> new ApplicationUser("bad-platform", "hash",
                ApplicationRole.PLATFORM_ADMIN, tenant)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ApplicationUser("bad-tenant", "hash",
                ApplicationRole.TENANT_ADMIN, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @RestController
    @RequestMapping("/api/tenant")
    static class TenantProbeController {
        private final TenantContext tenantContext;

        TenantProbeController(TenantContext tenantContext) {
            this.tenantContext = tenantContext;
        }

        @GetMapping("/context")
        Map<String, String> context() {
            return Map.of("tenantId", tenantContext.requireTenantId().toString());
        }
    }
}
