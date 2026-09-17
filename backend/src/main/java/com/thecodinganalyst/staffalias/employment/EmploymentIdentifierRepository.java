package com.thecodinganalyst.staffalias.employment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmploymentIdentifierRepository extends JpaRepository<EmploymentIdentifier, UUID> {
    Optional<EmploymentIdentifier> findByIdAndTenantId(UUID id, UUID tenantId);
    List<EmploymentIdentifier> findAllByTenantIdAndEmploymentIdOrderByEffectiveFrom(UUID tenantId, UUID employmentId);
}
