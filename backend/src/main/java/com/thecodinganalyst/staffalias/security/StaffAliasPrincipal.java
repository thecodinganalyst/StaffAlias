package com.thecodinganalyst.staffalias.security;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record StaffAliasPrincipal(
        UUID userId,
        String username,
        String password,
        ApplicationRole role,
        UUID tenantId,
        boolean enabled) implements UserDetails {

    public static StaffAliasPrincipal from(ApplicationUser user) {
        UUID tenantId = user.getTenant() == null ? null : user.getTenant().getId();
        return new StaffAliasPrincipal(user.getId(), user.getUsername(), user.getPasswordHash(),
                user.getRole(), tenantId, user.isEnabled());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }
}
