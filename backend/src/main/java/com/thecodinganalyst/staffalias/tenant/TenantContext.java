package com.thecodinganalyst.staffalias.tenant;

import java.util.UUID;

public interface TenantContext {
    UUID requireTenantId();
    void setTenantId(UUID tenantId);
    void clear();
}
