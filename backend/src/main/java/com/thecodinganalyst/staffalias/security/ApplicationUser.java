package com.thecodinganalyst.staffalias.security;

import java.util.UUID;

import com.thecodinganalyst.staffalias.tenant.Tenant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "application_user")
public class ApplicationUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 200)
    private String username;

    @Column(length = 320)
    private String email;

    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ApplicationRole role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @Column(nullable = false)
    private boolean enabled = true;

    protected ApplicationUser() {
    }

    public ApplicationUser(String username, String passwordHash, ApplicationRole role, Tenant tenant) {
        if (role == ApplicationRole.PLATFORM_ADMIN && tenant != null) {
            throw new IllegalArgumentException("Platform admin must not belong to a tenant");
        }
        if (role == ApplicationRole.TENANT_ADMIN && tenant == null) {
            throw new IllegalArgumentException("Tenant admin must belong to a tenant");
        }
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.tenant = tenant;
    }

    public UUID getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getEmail() { return email; }
    public ApplicationRole getRole() { return role; }
    public Tenant getTenant() { return tenant; }
    public boolean isEnabled() { return enabled; }
    public void setEmail(String email) { this.email = email; }
    public void disable() { this.enabled = false; }
    public void activate(String passwordHash) {
        this.passwordHash = passwordHash;
        this.enabled = true;
    }
}
