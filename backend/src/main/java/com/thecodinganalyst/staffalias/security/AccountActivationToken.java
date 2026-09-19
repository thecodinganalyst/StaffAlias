package com.thecodinganalyst.staffalias.security;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;

@Entity
@Table(name = "account_activation_token")
public class AccountActivationToken {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false)
    private ApplicationUser user;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64) private String tokenHash;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "used_at") private Instant usedAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected AccountActivationToken() {}
    public AccountActivationToken(ApplicationUser user, String tokenHash, Instant expiresAt) {
        this.id = UUID.randomUUID(); this.user = user; this.tokenHash = tokenHash;
        this.expiresAt = expiresAt; this.createdAt = Instant.now();
    }
    public ApplicationUser getUser() { return user; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getUsedAt() { return usedAt; }
    public void markUsed() { usedAt = Instant.now(); }
}
