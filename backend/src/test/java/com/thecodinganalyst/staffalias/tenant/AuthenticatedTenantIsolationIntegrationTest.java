package com.thecodinganalyst.staffalias.tenant;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import com.thecodinganalyst.staffalias.employment.Employment;
import com.thecodinganalyst.staffalias.employment.EmploymentIdentifier;
import com.thecodinganalyst.staffalias.employment.EmploymentIdentifierRepository;
import com.thecodinganalyst.staffalias.employment.EmploymentRepository;
import com.thecodinganalyst.staffalias.employment.StaffLifecycleService;
import com.thecodinganalyst.staffalias.people.Person;
import com.thecodinganalyst.staffalias.people.PersonRepository;
import com.thecodinganalyst.staffalias.security.ApplicationRole;
import com.thecodinganalyst.staffalias.security.ApplicationUser;
import com.thecodinganalyst.staffalias.security.ApplicationUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@Import(AuthenticatedTenantIsolationIntegrationTest.TenantProbeController.class)
class AuthenticatedTenantIsolationIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired MockMvc mockMvc;
    @Autowired ApplicationUserRepository userRepository;
    @Autowired TenantRepository tenantRepository;
    @Autowired TenantSettingRepository settingRepository;
    @Autowired PersonRepository personRepository;
    @Autowired EmploymentRepository employmentRepository;
    @Autowired EmploymentIdentifierRepository identifierRepository;
    @Autowired TenantSettingService settingService;
    @Autowired StaffLifecycleService lifecycleService;
    @Autowired TenantContext tenantContext;
    @Autowired PasswordEncoder passwordEncoder;

    private Tenant tenantA;
    private Tenant tenantB;
    private TenantSetting settingB;
    private Person personB;
    private Employment employmentB;
    private EmploymentIdentifier identifierB;

    @BeforeEach
    void setUp() {
        identifierRepository.deleteAll();
        employmentRepository.deleteAll();
        personRepository.deleteAll();
        settingRepository.deleteAll();
        userRepository.deleteAll();
        tenantRepository.deleteAll();

        tenantA = tenantRepository.save(new Tenant("TENANT_A", "Tenant A"));
        tenantB = tenantRepository.save(new Tenant("TENANT_B", "Tenant B"));
        userRepository.save(new ApplicationUser("admin-a", passwordEncoder.encode("password-a"),
                ApplicationRole.TENANT_ADMIN, tenantA));
        userRepository.save(new ApplicationUser("admin-b", passwordEncoder.encode("password-b"),
                ApplicationRole.TENANT_ADMIN, tenantB));
        userRepository.save(new ApplicationUser("platform", passwordEncoder.encode("platform-pass"),
                ApplicationRole.PLATFORM_ADMIN, null));

        tenantContext.setTenantId(tenantB.getId());
        settingB = settingService.create("region", "B");
        personB = lifecycleService.createPerson("Tenant", "Bee");
        employmentB = lifecycleService.createEmployment(personB.getId(), LocalDate.of(2026, 1, 1), null, "B-001");
        identifierB = lifecycleService.identifiersForEmployment(employmentB.getId()).get(0);
        tenantContext.clear();
    }

    @AfterEach
    void clearContext() {
        tenantContext.clear();
    }

    @Test
    void tenantAdminCanAccessOwnTenantResources() throws Exception {
        mockMvc.perform(get("/api/tenant/probe/people/{id}", personB.getId())
                        .with(httpBasic("admin-b", "password-b")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(personB.getId().toString()));

        mockMvc.perform(get("/api/tenant/probe/employments/{id}", employmentB.getId())
                        .with(httpBasic("admin-b", "password-b")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(employmentB.getId().toString()));

        mockMvc.perform(get("/api/tenant/probe/identifiers/{id}", identifierB.getId())
                        .with(httpBasic("admin-b", "password-b")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(identifierB.getId().toString()));
    }

    @Test
    void tenantAdminCannotReadAnotherTenantsPersonEmploymentIdentifierOrSetting() throws Exception {
        mockMvc.perform(get("/api/tenant/probe/people/{id}", personB.getId())
                        .with(httpBasic("admin-a", "password-a")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/tenant/probe/employments/{id}", employmentB.getId())
                        .with(httpBasic("admin-a", "password-a")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/tenant/probe/identifiers/{id}", identifierB.getId())
                        .with(httpBasic("admin-a", "password-a")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/tenant/probe/settings/{id}", settingB.getId())
                        .with(httpBasic("admin-a", "password-a")))
                .andExpect(status().isNotFound());
    }

    @Test
    void tenantAdminCannotUpdateOrDeleteAnotherTenantsSetting() throws Exception {
        mockMvc.perform(put("/api/tenant/probe/settings/{id}", settingB.getId())
                        .with(httpBasic("admin-a", "password-a"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":\"hijacked\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/tenant/probe/settings/{id}", settingB.getId())
                        .with(httpBasic("admin-a", "password-a")))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/tenant/probe/settings/{id}", settingB.getId())
                        .with(httpBasic("admin-b", "password-b")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value("B"));
    }

    @Test
    void clientSuppliedTenantIdentifiersCannotOverrideAuthenticatedTenant() throws Exception {
        mockMvc.perform(post("/api/tenant/probe/people")
                        .with(httpBasic("admin-a", "password-a"))
                        .header("X-Tenant-Id", tenantB.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Alice\",\"lastName\":\"A\",\"tenantId\":\""
                                + tenantB.getId() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value(tenantA.getId().toString()));
    }

    @Test
    void tenantAdminCannotCreateEmploymentForAnotherTenantsPerson() throws Exception {
        mockMvc.perform(post("/api/tenant/probe/people/{id}/employments", personB.getId())
                        .with(httpBasic("admin-a", "password-a"))
                        .header("X-Tenant-Id", tenantB.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeId\":\"A-999\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void platformAdminCannotUseTenantUserWorkflow() throws Exception {
        mockMvc.perform(get("/api/tenant/probe/settings/{id}", settingB.getId())
                        .with(httpBasic("platform", "platform-pass")))
                .andExpect(status().isForbidden());
    }

    @RestController
    @RequestMapping("/api/tenant/probe")
    static class TenantProbeController {
        private final TenantSettingService settings;
        private final StaffLifecycleService lifecycle;

        TenantProbeController(TenantSettingService settings, StaffLifecycleService lifecycle) {
            this.settings = settings;
            this.lifecycle = lifecycle;
        }

        @GetMapping("/settings/{id}")
        Map<String, Object> setting(@PathVariable UUID id) {
            TenantSetting setting = settings.get(id);
            return Map.of("id", setting.getId(), "value", setting.getValue());
        }

        @PutMapping("/settings/{id}")
        Map<String, Object> updateSetting(@PathVariable UUID id, @RequestBody Map<String, String> body) {
            TenantSetting setting = settings.update(id, body.get("value"));
            return Map.of("id", setting.getId(), "value", setting.getValue());
        }

        @DeleteMapping("/settings/{id}")
        void deleteSetting(@PathVariable UUID id) {
            settings.delete(id);
        }

        @GetMapping("/people/{id}")
        Map<String, Object> person(@PathVariable UUID id) {
            Person person = lifecycle.getPerson(id);
            return Map.of("id", person.getId(), "tenantId", person.getTenant().getId());
        }

        @PostMapping("/people")
        Map<String, Object> createPerson(@RequestBody Map<String, String> body) {
            Person person = lifecycle.createPerson(body.get("firstName"), body.get("lastName"));
            return Map.of("id", person.getId(), "tenantId", person.getTenant().getId());
        }

        @GetMapping("/employments/{id}")
        Map<String, Object> employment(@PathVariable UUID id) {
            Employment employment = lifecycle.getEmployment(id);
            return Map.of("id", employment.getId(), "tenantId", employment.getTenant().getId());
        }

        @GetMapping("/identifiers/{id}")
        Map<String, Object> identifier(@PathVariable UUID id) {
            EmploymentIdentifier identifier = lifecycle.getIdentifier(id);
            return Map.of("id", identifier.getId(), "employeeId", identifier.getEmployeeId());
        }

        @PostMapping("/people/{id}/employments")
        Map<String, Object> createEmployment(@PathVariable UUID id, @RequestBody Map<String, String> body) {
            Employment employment = lifecycle.createEmployment(id, LocalDate.now(), null, body.get("employeeId"));
            return Map.of("id", employment.getId(), "tenantId", employment.getTenant().getId());
        }
    }
}
