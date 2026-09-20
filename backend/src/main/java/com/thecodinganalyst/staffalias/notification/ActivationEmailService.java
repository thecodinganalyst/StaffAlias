package com.thecodinganalyst.staffalias.notification;

public interface ActivationEmailService {
    default boolean isConfigured() { return true; }

    boolean sendTenantAdminActivation(String email, String tenantName, String activationUrl);
}
