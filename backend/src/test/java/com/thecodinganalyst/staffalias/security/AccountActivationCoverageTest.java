package com.thecodinganalyst.staffalias.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import com.thecodinganalyst.staffalias.notification.ActivationEmailService;
import com.thecodinganalyst.staffalias.notification.ResendActivationEmailService;
import com.thecodinganalyst.staffalias.tenant.Tenant;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

class AccountActivationCoverageTest {

    @Test
    void issuePersistsHashedTokenAndSendsActivationEmail() {
        AccountActivationTokenRepository tokens = mock(AccountActivationTokenRepository.class);
        ApplicationUserRepository users = mock(ApplicationUserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        ActivationEmailService email = mock(ActivationEmailService.class);
        AccountActivationService service = new AccountActivationService(tokens, users, encoder, email,
                "https://staffalias.example/");

        ApplicationUser user = pendingAdmin();
        when(email.isConfigured()).thenReturn(true);
        when(email.sendTenantAdminActivation(eq("admin@example.com"), eq("Acme"), anyString())).thenReturn(true);

        assertThat(service.issue(user, "Acme")).isTrue();
        verify(tokens).save(any(AccountActivationToken.class));
        verify(email).sendTenantAdminActivation(eq("admin@example.com"), eq("Acme"),
                org.mockito.ArgumentMatchers.contains("https://staffalias.example/activate?token="));
    }

    @Test
    void issueSkipsTokenPersistenceWhenEmailIsNotConfigured() {
        AccountActivationTokenRepository tokens = mock(AccountActivationTokenRepository.class);
        ActivationEmailService email = mock(ActivationEmailService.class);
        AccountActivationService service = new AccountActivationService(tokens,
                mock(ApplicationUserRepository.class), mock(PasswordEncoder.class), email,
                "https://staffalias.example/");

        when(email.isConfigured()).thenReturn(false);

        assertThat(service.issue(pendingAdmin(), "Acme")).isFalse();
        verify(email).isConfigured();
        org.mockito.Mockito.verifyNoInteractions(tokens);
    }

    @Test
    void activateEnablesUserAndConsumesToken() {
        AccountActivationTokenRepository tokens = mock(AccountActivationTokenRepository.class);
        ApplicationUserRepository users = mock(ApplicationUserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        ActivationEmailService email = mock(ActivationEmailService.class);
        AccountActivationService service = new AccountActivationService(tokens, users, encoder, email,
                "http://localhost:5173");

        ApplicationUser user = pendingAdmin();
        AccountActivationToken token = new AccountActivationToken(user, "hash", Instant.now().plusSeconds(600));
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        when(encoder.encode("a-secure-password")).thenReturn("encoded");

        service.activate("raw-token", "a-secure-password");

        assertThat(user.isEnabled()).isTrue();
        assertThat(user.getPasswordHash()).isEqualTo("encoded");
        assertThat(token.getUsedAt()).isNotNull();
        assertThat(token.getUser()).isSameAs(user);
        assertThat(token.getExpiresAt()).isAfter(Instant.now());
        verify(users).save(user);
        verify(tokens).save(token);
    }

    @Test
    void invalidExpiredAndUsedTokensAreRejected() {
        AccountActivationTokenRepository tokens = mock(AccountActivationTokenRepository.class);
        AccountActivationService service = new AccountActivationService(tokens,
                mock(ApplicationUserRepository.class), mock(PasswordEncoder.class),
                mock(ActivationEmailService.class), "http://localhost:5173");

        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.empty());
        assertBadToken(() -> service.activate("missing", "a-secure-password"));

        ApplicationUser user = pendingAdmin();
        AccountActivationToken expired = new AccountActivationToken(user, "expired", Instant.now().minusSeconds(1));
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.of(expired));
        assertBadToken(() -> service.activate("expired", "a-secure-password"));

        AccountActivationToken used = new AccountActivationToken(user, "used", Instant.now().plusSeconds(600));
        used.markUsed();
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.of(used));
        assertBadToken(() -> service.activate("used", "a-secure-password"));
    }

    @Test
    void controllerDelegatesAndReturnsNoContent() {
        AccountActivationService service = mock(AccountActivationService.class);
        AccountActivationController controller = new AccountActivationController(service);
        var response = controller.activate(new AccountActivationController.ActivationRequest(
                "token", "a-secure-password"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(service).activate("token", "a-secure-password");
    }

    @Test
    void resendIsOptionalWhenConfigurationIsMissing() {
        ResendActivationEmailService service = new ResendActivationEmailService(" ", " ");
        assertThat(service.isConfigured()).isFalse();
        assertThat(service.sendTenantAdminActivation("admin@example.com", "Acme",
                "https://example.test/activate")).isFalse();
    }

    @Test
    void resendReportsConfiguredWhenRequiredSettingsExist() {
        ResendActivationEmailService service = new ResendActivationEmailService("test-key", "StaffAlias <noreply@example.test>");
        assertThat(service.isConfigured()).isTrue();
    }

    private ApplicationUser pendingAdmin() {
        ApplicationUser user = new ApplicationUser("admin@example.com", null, ApplicationRole.TENANT_ADMIN,
                new Tenant("ACME", "Acme"));
        user.setEmail("admin@example.com");
        user.disable();
        return user;
    }

    private void assertBadToken(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }
}
