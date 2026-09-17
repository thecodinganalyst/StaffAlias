package com.thecodinganalyst.staffalias.platform;

import com.thecodinganalyst.staffalias.security.ApplicationRole;
import com.thecodinganalyst.staffalias.security.ApplicationUser;
import com.thecodinganalyst.staffalias.security.ApplicationUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PlatformAdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PlatformAdminBootstrap.class);

    private final ApplicationUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;

    public PlatformAdminBootstrap(ApplicationUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${staffalias.platform-admin.username:}") String username,
            @Value("${staffalias.platform-admin.password:}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(username) && !StringUtils.hasText(password)) {
            log.info("Platform admin bootstrap is not configured");
            return;
        }
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new IllegalStateException("Both platform admin username and password must be configured together");
        }

        var existing = userRepository.findByUsernameIgnoreCase(username);
        if (existing.isPresent()) {
            if (existing.get().getRole() != ApplicationRole.PLATFORM_ADMIN) {
                throw new IllegalStateException("Configured platform admin username belongs to a non-platform account");
            }
            log.info("Platform admin bootstrap skipped because account already exists");
            return;
        }

        userRepository.save(new ApplicationUser(
                username,
                passwordEncoder.encode(password),
                ApplicationRole.PLATFORM_ADMIN,
                null));
        log.info("Platform admin account bootstrapped successfully");
    }
}
