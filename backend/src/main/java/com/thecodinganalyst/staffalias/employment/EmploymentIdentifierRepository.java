package com.thecodinganalyst.staffalias.employment;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmploymentIdentifierRepository extends JpaRepository<EmploymentIdentifier, UUID> {
    List<EmploymentIdentifier> findAllByTenantIdAndEmploymentIdOrderByEffectiveFrom(UUID tenantId, UUID employmentId);
}
