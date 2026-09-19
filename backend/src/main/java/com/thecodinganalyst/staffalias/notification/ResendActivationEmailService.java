package com.thecodinganalyst.staffalias.notification;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class ResendActivationEmailService implements ActivationEmailService {
    private static final Logger log = LoggerFactory.getLogger(ResendActivationEmailService.class);
    private final String apiKey;
    private final String from;
    private final RestClient restClient;

    public ResendActivationEmailService(
            @Value("${staffalias.email.resend.api-key:}") String apiKey,
            @Value("${staffalias.email.from:}") String from) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.from = from == null ? "" : from.trim();
        this.restClient = RestClient.builder().baseUrl("https://api.resend.com").build();
    }

    @Override
    public boolean sendTenantAdminActivation(String email, String tenantName, String activationUrl) {
        if (apiKey.isBlank() || from.isBlank()) {
            log.warn("Tenant admin activation email not sent because Resend is not configured; recipient={} tenant={}",
                    email, tenantName);
            return false;
        }
        try {
            restClient.post().uri("/emails")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "from", from,
                            "to", new String[]{email},
                            "subject", "Activate your StaffAlias tenant administrator account",
                            "html", "<p>You have been invited to administer <strong>" + escape(tenantName)
                                    + "</strong> in StaffAlias.</p><p><a href=\"" + escape(activationUrl)
                                    + "\">Set your password and activate your account</a></p>"
                                    + "<p>This link expires in 24 hours and can only be used once.</p>"))
                    .retrieve().toBodilessEntity();
            log.info("Tenant admin activation email submitted to Resend; recipient={} tenant={}", email, tenantName);
            return true;
        } catch (RuntimeException ex) {
            log.error("Unable to send tenant admin activation email; tenant creation remains successful; recipient={} tenant={}",
                    email, tenantName, ex);
            return false;
        }
    }

    private String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
