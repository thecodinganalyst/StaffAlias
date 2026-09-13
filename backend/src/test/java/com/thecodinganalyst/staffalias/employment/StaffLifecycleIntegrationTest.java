package com.thecodinganalyst.staffalias.employment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import com.thecodinganalyst.staffalias.people.Person;
import com.thecodinganalyst.staffalias.people.PersonRepository;
import com.thecodinganalyst.staffalias.tenant.Tenant;
import com.thecodinganalyst.staffalias.tenant.TenantContext;
import com.thecodinganalyst.staffalias.tenant.TenantRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
class StaffLifecycleIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired TenantRepository tenantRepository;
    @Autowired PersonRepository personRepository;
    @Autowired EmploymentRepository employmentRepository;
    @Autowired EmploymentIdentifierRepository identifierRepository;
    @Autowired StaffLifecycleService lifecycleService;
    @Autowired TenantContext tenantContext;

    private Tenant tenantA;
    private Tenant tenantB;

    @BeforeEach
    void setUp() {
        identifierRepository.deleteAll();
        employmentRepository.deleteAll();
        personRepository.deleteAll();
        tenantRepository.deleteAll();
        tenantA = tenantRepository.save(new Tenant("TENANT_A", "Tenant A"));
        tenantB = tenantRepository.save(new Tenant("TENANT_B", "Tenant B"));
    }

    @AfterEach
    void clearTenant() {
        tenantContext.clear();
    }

    @Test
    void samePersonCanBeRehiredWithSameEmployeeId() {
        tenantContext.setTenantId(tenantA.getId());
        Person person = lifecycleService.createPerson("Alex", "Tan");

        lifecycleService.createEmployment(person.getId(), LocalDate.of(2020, 1, 1),
                LocalDate.of(2022, 12, 31), "E100");
        lifecycleService.createEmployment(person.getId(), LocalDate.of(2024, 1, 1), null, "E100");

        assertThat(lifecycleService.employmentsForPerson(person.getId())).hasSize(2);
    }

    @Test
    void samePersonCanBeRehiredWithDifferentEmployeeId() {
        tenantContext.setTenantId(tenantA.getId());
        Person person = lifecycleService.createPerson("Jamie", "Lee");

        lifecycleService.createEmployment(person.getId(), LocalDate.of(2019, 1, 1),
                LocalDate.of(2021, 6, 30), "E200");
        Employment rehire = lifecycleService.createEmployment(person.getId(), LocalDate.of(2023, 1, 1), null, "E900");

        assertThat(lifecycleService.identifiersForEmployment(rehire.getId()))
                .extracting(EmploymentIdentifier::getEmployeeId)
                .containsExactly("E900");
    }

    @Test
    void differentTenantsCanUseSameEmployeeId() {
        tenantContext.setTenantId(tenantA.getId());
        Person a = lifecycleService.createPerson("A", "User");
        lifecycleService.createEmployment(a.getId(), LocalDate.of(2024, 1, 1), null, "E001");

        tenantContext.setTenantId(tenantB.getId());
        Person b = lifecycleService.createPerson("B", "User");
        lifecycleService.createEmployment(b.getId(), LocalDate.of(2024, 1, 1), null, "E001");

        assertThat(identifierRepository.count()).isEqualTo(2);
    }

    @Test
    void overlappingEmployeeIdUseInSameTenantIsRejected() {
        tenantContext.setTenantId(tenantA.getId());
        Person first = lifecycleService.createPerson("First", "User");
        lifecycleService.createEmployment(first.getId(), LocalDate.of(2024, 1, 1), null, "E777");

        Person second = lifecycleService.createPerson("Second", "User");

        assertThatThrownBy(() -> lifecycleService.createEmployment(
                second.getId(), LocalDate.of(2025, 1, 1), null, "E777"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
