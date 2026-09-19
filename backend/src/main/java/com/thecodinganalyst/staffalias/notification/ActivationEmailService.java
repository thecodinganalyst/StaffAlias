package com.thecodinganalyst.staffalias.notification;

public interface ActivationEmailService {
    boolean sendTenantAdminActivation(String email, String tenantName, String activationUrl);
}
