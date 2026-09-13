package com.thecodinganalyst.staffalias.employment;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.thecodinganalyst.staffalias.tenant.Tenant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "employment_identifier")
public class EmploymentIdentifier {

    public static final String EMPLOYEE_ID = "EMPLOYEE_ID";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employment_id", nullable = false)
    private Employment employment;

    @Column(name = "identifier_type", nullable = false, length = 32)
    private String identifierType;

    @Column(name = "employee_id", nullable = false, length = 100)
    private String employeeId;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected EmploymentIdentifier() {
    }

    public EmploymentIdentifier(Tenant tenant, Employment employment, String employeeId,
            LocalDate effectiveFrom, LocalDate effectiveTo) {
        this.tenant = tenant;
        this.employment = employment;
        this.identifierType = EMPLOYEE_ID;
        this.employeeId = employeeId;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
    }

    public UUID getId() { return id; }
    public Employment getEmployment() { return employment; }
    public String getIdentifierType() { return identifierType; }
    public String getEmployeeId() { return employeeId; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public LocalDate getEffectiveTo() { return effectiveTo; }
}
