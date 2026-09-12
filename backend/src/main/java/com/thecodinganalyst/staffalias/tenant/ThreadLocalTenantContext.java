package com.thecodinganalyst.staffalias.tenant;

import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class ThreadLocalTenantContext implements TenantContext {

    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    @Override
    public UUID requireTenantId() {
        UUID tenantId = CURRENT.get();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context has not been established");
        }
        return tenantId;
    }

    @Override
    public void setTenantId(UUID tenantId) {
        CURRENT.set(tenantId);
    }

    @Override
    public void clear() {
        CURRENT.remove();
    }
}
