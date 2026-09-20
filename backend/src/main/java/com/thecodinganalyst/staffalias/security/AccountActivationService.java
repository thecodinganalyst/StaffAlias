package com.thecodinganalyst.staffalias.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import com.thecodinganalyst.staffalias.notification.ActivationEmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountActivationService {
    private final AccountActivationTokenRepository tokenRepository;
    private final ApplicationUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ActivationEmailService emailService;
    private final String frontendUrl;
    private final SecureRandom random = new SecureRandom();

    public AccountActivationService(AccountActivationTokenRepository tokenRepository,
            ApplicationUserRepository userRepository, PasswordEncoder passwordEncoder,
            ActivationEmailService emailService,
            @Value("${staffalias.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.tokenRepository = tokenRepository; this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder; this.emailService = emailService;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    public boolean issue(ApplicationUser user, String tenantName) {
        // Email delivery is optional. When no provider is configured there is no
        // usable recipient path for a raw token, so do not let token persistence
        // become a dependency of tenant provisioning.
        if (!emailService.isConfigured()) return false;

        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokenRepository.save(new AccountActivationToken(user, hash(raw), Instant.now().plus(Duration.ofHours(24))));
        return emailService.sendTenantAdminActivation(user.getEmail(), tenantName,
                frontendUrl + "/activate?token=" + raw);
    }

    @Transactional
    public void activate(String rawToken, String password) {
        AccountActivationToken token = tokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> invalidToken());
        if (token.getUsedAt() != null || !token.getExpiresAt().isAfter(Instant.now())) throw invalidToken();
        ApplicationUser user = token.getUser();
        user.activate(passwordEncoder.encode(password));
        userRepository.save(user);
        token.markUsed();
        tokenRepository.save(token);
    }

    private ResponseStatusException invalidToken() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Activation link is invalid or expired");
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) { throw new IllegalStateException("SHA-256 unavailable", ex); }
    }
}
