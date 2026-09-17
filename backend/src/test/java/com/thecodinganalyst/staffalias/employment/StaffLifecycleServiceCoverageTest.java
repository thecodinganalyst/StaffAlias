package com.thecodinganalyst.staffalias.employment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.thecodinganalyst.staffalias.people.Person;
import com.thecodinganalyst.staffalias.people.PersonRepository;
import com.thecodinganalyst.staffalias.tenant.Tenant;
import com.thecodinganalyst.staffalias.tenant.TenantContext;
import com.thecodinganalyst.staffalias.tenant.TenantRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StaffLifecycleServiceCoverageTest {

    private TenantContext tenantContext;
    private TenantRepository tenantRepository;
    private PersonRepository personRepository;
    private EmploymentRepository employmentRepository;
    private EmploymentIdentifierRepository identifierRepository;
    private StaffLifecycleService service;
    private UUID tenantId;
    private Tenant tenant;

    @BeforeEach
    void setUp() {
        tenantContext = mock(TenantContext.class);
        tenantRepository = mock(TenantRepository.class);
        personRepository = mock(PersonRepository.class);
        employmentRepository = mock(EmploymentRepository.class);
        identifierRepository = mock(EmploymentIdentifierRepository.class);
        service = new StaffLifecycleService(tenantContext, tenantRepository, personRepository,
                employmentRepository, identifierRepository);
        tenantId = UUID.randomUUID();
        tenant = new Tenant("TENANT", "Tenant");
        when(tenantContext.requireTenantId()).thenReturn(tenantId);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
    }

    @Test
    void domainGettersAndLifecycleSuccessPathsAreCovered() {
        Person person = new Person(tenant, "Alex", "Tan");
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2025, 12, 31);
        Employment employment = new Employment(tenant, person, start, end, EmploymentStatus.ENDED);
        EmploymentIdentifier identifier = new EmploymentIdentifier(tenant, employment, "E100", start, end);

        assertThat(employment.getId()).isNull();
        assertThat(employment.getTenant()).isSameAs(tenant);
        assertThat(employment.getPerson()).isSameAs(person);
        assertThat(employment.getStartDate()).isEqualTo(start);
        assertThat(employment.getEndDate()).isEqualTo(end);
        assertThat(employment.getStatus()).isEqualTo(EmploymentStatus.ENDED);
        assertThat(identifier.getId()).isNull();
        assertThat(identifier.getEmployment()).isSameAs(employment);
        assertThat(identifier.getIdentifierType()).isEqualTo(EmploymentIdentifier.EMPLOYEE_ID);
        assertThat(identifier.getEmployeeId()).isEqualTo("E100");
        assertThat(identifier.getEffectiveFrom()).isEqualTo(start);
        assertThat(identifier.getEffectiveTo()).isEqualTo(end);

        when(personRepository.save(any(Person.class))).thenAnswer(i -> i.getArgument(0));
        Person created = service.createPerson("Jamie", "Lee");
        assertThat(created.getFirstName()).isEqualTo("Jamie");

        UUID personId = UUID.randomUUID();
        when(personRepository.findByIdAndTenantId(personId, tenantId)).thenReturn(Optional.of(person));
        when(employmentRepository.save(any(Employment.class))).thenAnswer(i -> i.getArgument(0));
        when(identifierRepository.save(any(EmploymentIdentifier.class))).thenAnswer(i -> i.getArgument(0));
        Employment active = service.createEmployment(personId, start, null, "E200");
        assertThat(active.getStatus()).isEqualTo(EmploymentStatus.ACTIVE);
        Employment ended = service.createEmployment(personId, start, end, "E201");
        assertThat(ended.getStatus()).isEqualTo(EmploymentStatus.ENDED);

        UUID employmentId = UUID.randomUUID();
        when(employmentRepository.findByIdAndTenantId(employmentId, tenantId)).thenReturn(Optional.of(employment));
        when(identifierRepository.saveAndFlush(any(EmploymentIdentifier.class))).thenAnswer(i -> i.getArgument(0));
        assertThat(service.addEmployeeId(employmentId, "E300", start, null).getEmployeeId()).isEqualTo("E300");

        when(employmentRepository.findAllByTenantIdAndPersonIdOrderByStartDate(tenantId, personId))
                .thenReturn(List.of(employment));
        assertThat(service.employmentsForPerson(personId)).containsExactly(employment);
        when(identifierRepository.findAllByTenantIdAndEmploymentIdOrderByEffectiveFrom(tenantId, employmentId))
                .thenReturn(List.of(identifier));
        assertThat(service.identifiersForEmployment(employmentId)).containsExactly(identifier);
    }

    @Test
    void lifecycleMissingEntityPathsAreCovered() {
        UUID missingPerson = UUID.randomUUID();
        when(personRepository.findByIdAndTenantId(missingPerson, tenantId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.createEmployment(missingPerson, LocalDate.now(), null, "E1"))
                .isInstanceOf(EntityNotFoundException.class);
        assertThatThrownBy(() -> service.employmentsForPerson(missingPerson))
                .isInstanceOf(EntityNotFoundException.class);

        UUID missingEmployment = UUID.randomUUID();
        when(employmentRepository.findByIdAndTenantId(missingEmployment, tenantId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.addEmployeeId(missingEmployment, "E2", LocalDate.now(), null))
                .isInstanceOf(EntityNotFoundException.class);
        assertThatThrownBy(() -> service.identifiersForEmployment(missingEmployment))
                .isInstanceOf(EntityNotFoundException.class);

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.createPerson("No", "Tenant"))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
