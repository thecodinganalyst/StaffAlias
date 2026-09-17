package com.thecodinganalyst.staffalias.security;

import java.io.IOException;

import com.thecodinganalyst.staffalias.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class TenantContextSecurityFilter extends OncePerRequestFilter {

    private final TenantContext tenantContext;

    public TenantContextSecurityFilter(TenantContext tenantContext) {
        this.tenantContext = tenantContext;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && authentication.getPrincipal() instanceof StaffAliasPrincipal principal
                    && principal.role() == ApplicationRole.TENANT_ADMIN) {
                if (principal.tenantId() == null) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Tenant administrator has no tenant");
                    return;
                }
                tenantContext.setTenantId(principal.tenantId());
            }
            filterChain.doFilter(request, response);
        } finally {
            tenantContext.clear();
        }
    }
}
