package com.thecodinganalyst.staffalias.employment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmploymentRepository extends JpaRepository<Employment, UUID> {
    Optional<Employment> findByIdAndTenantId(UUID id, UUID tenantId);
    List<Employment> findAllByTenantIdAndPersonIdOrderByStartDate(UUID tenantId, UUID personId);
}
