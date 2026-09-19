package com.thecodinganalyst.staffalias.platform;

import java.util.List;
import java.util.UUID;

import com.thecodinganalyst.staffalias.security.ApplicationRole;
import com.thecodinganalyst.staffalias.security.ApplicationUser;
import com.thecodinganalyst.staffalias.security.ApplicationUserRepository;
import com.thecodinganalyst.staffalias.tenant.Tenant;
import com.thecodinganalyst.staffalias.tenant.TenantRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class PlatformTenantAdminService {

    private static final Logger log = LoggerFactory.getLogger(PlatformTenantAdminService.class);

    private final TenantRepository tenantRepository;
    private final ApplicationUserRepository userRepository;
    private final AccountActivationService activationService;

    public PlatformTenantAdminService(TenantRepository tenantRepository,
            ApplicationUserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.activationService = activationService;
    }

    @Transactional(readOnly = true)
    public List<Tenant> listTenants() {
        return tenantRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Tenant getTenant(UUID id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));
    }

    public TenantProvisioningResult provisionTenant(String code, String name,
            String adminUsername, String initialPassword) {
        if (tenantRepository.findByCode(code).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tenant code already exists");
        }
        if (userRepository.findByUsernameIgnoreCase(adminUsername).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }

        Tenant tenant = tenantRepository.save(new Tenant(code, name));
        ApplicationUser tenantAdmin = userRepository.save(new ApplicationUser(
                adminUsername,
                passwordEncoder.encode(initialPassword),
                ApplicationRole.TENANT_ADMIN,
                tenant));

        log.info("Platform administration provisioned tenant id={} code={} adminUserId={}",
                tenant.getId(), tenant.getCode(), tenantAdmin.getId());
        return new TenantProvisioningResult(tenant, tenantAdmin, activationEmailSent);
    }

    public record TenantProvisioningResult(Tenant tenant, ApplicationUser tenantAdmin, boolean activationEmailSent) {
    }
}
