package com.thecodinganalyst.staffalias.employment;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.thecodinganalyst.staffalias.people.Person;
import com.thecodinganalyst.staffalias.people.PersonRepository;
import com.thecodinganalyst.staffalias.tenant.Tenant;
import com.thecodinganalyst.staffalias.tenant.TenantContext;
import com.thecodinganalyst.staffalias.tenant.TenantRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StaffLifecycleService {

    private final TenantContext tenantContext;
    private final TenantRepository tenantRepository;
    private final PersonRepository personRepository;
    private final EmploymentRepository employmentRepository;
    private final EmploymentIdentifierRepository identifierRepository;

    public StaffLifecycleService(TenantContext tenantContext, TenantRepository tenantRepository,
            PersonRepository personRepository, EmploymentRepository employmentRepository,
            EmploymentIdentifierRepository identifierRepository) {
        this.tenantContext = tenantContext;
        this.tenantRepository = tenantRepository;
        this.personRepository = personRepository;
        this.employmentRepository = employmentRepository;
        this.identifierRepository = identifierRepository;
    }

    public Person createPerson(String firstName, String lastName) {
        Tenant tenant = currentTenant();
        return personRepository.save(new Person(tenant, firstName, lastName));
    }

    @Transactional(readOnly = true)
    public Person getPerson(UUID personId) {
        UUID tenantId = tenantContext.requireTenantId();
        return personRepository.findByIdAndTenantId(personId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Person not found"));
    }

    public Employment createEmployment(UUID personId, LocalDate startDate, LocalDate endDate,
            String employeeId) {
        UUID tenantId = tenantContext.requireTenantId();
        Tenant tenant = currentTenant();
        Person person = personRepository.findByIdAndTenantId(personId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Person not found"));

        EmploymentStatus status = endDate == null ? EmploymentStatus.ACTIVE : EmploymentStatus.ENDED;
        Employment employment = employmentRepository.save(
                new Employment(tenant, person, startDate, endDate, status));
        identifierRepository.save(new EmploymentIdentifier(
                tenant, employment, employeeId, startDate, endDate));
        return employment;
    }

    @Transactional(readOnly = true)
    public Employment getEmployment(UUID employmentId) {
        UUID tenantId = tenantContext.requireTenantId();
        return employmentRepository.findByIdAndTenantId(employmentId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Employment not found"));
    }

    public EmploymentIdentifier addEmployeeId(UUID employmentId, String employeeId,
            LocalDate effectiveFrom, LocalDate effectiveTo) {
        UUID tenantId = tenantContext.requireTenantId();
        Tenant tenant = currentTenant();
        Employment employment = employmentRepository.findByIdAndTenantId(employmentId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Employment not found"));
        return identifierRepository.saveAndFlush(new EmploymentIdentifier(
                tenant, employment, employeeId, effectiveFrom, effectiveTo));
    }

    @Transactional(readOnly = true)
    public EmploymentIdentifier getIdentifier(UUID identifierId) {
        UUID tenantId = tenantContext.requireTenantId();
        return identifierRepository.findByIdAndTenantId(identifierId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Employment identifier not found"));
    }

    @Transactional(readOnly = true)
    public List<Employment> employmentsForPerson(UUID personId) {
        UUID tenantId = tenantContext.requireTenantId();
        if (personRepository.findByIdAndTenantId(personId, tenantId).isEmpty()) {
            throw new EntityNotFoundException("Person not found");
        }
        return employmentRepository.findAllByTenantIdAndPersonIdOrderByStartDate(tenantId, personId);
    }

    @Transactional(readOnly = true)
    public List<EmploymentIdentifier> identifiersForEmployment(UUID employmentId) {
        UUID tenantId = tenantContext.requireTenantId();
        if (employmentRepository.findByIdAndTenantId(employmentId, tenantId).isEmpty()) {
            throw new EntityNotFoundException("Employment not found");
        }
        return identifierRepository.findAllByTenantIdAndEmploymentIdOrderByEffectiveFrom(tenantId, employmentId);
    }

    private Tenant currentTenant() {
        UUID tenantId = tenantContext.requireTenantId();
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));
    }
}
