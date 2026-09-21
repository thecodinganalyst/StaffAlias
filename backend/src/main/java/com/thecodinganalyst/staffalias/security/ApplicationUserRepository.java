package com.thecodinganalyst.staffalias.security;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationUserRepository extends JpaRepository<ApplicationUser, UUID> {
    Optional<ApplicationUser> findByUsernameIgnoreCase(String username);
    java.util.List<ApplicationUser> findByTenantId(UUID tenantId);
}
