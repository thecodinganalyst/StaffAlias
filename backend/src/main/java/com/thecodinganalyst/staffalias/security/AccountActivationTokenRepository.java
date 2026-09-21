package com.thecodinganalyst.staffalias.security;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountActivationTokenRepository extends JpaRepository<AccountActivationToken, UUID> {
    Optional<AccountActivationToken> findByTokenHash(String tokenHash);
}
