package com.thecodinganalyst.staffalias.platform;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.thecodinganalyst.staffalias.security.AccountActivationService;
import com.thecodinganalyst.staffalias.security.ApplicationRole;
import com.thecodinganalyst.staffalias.security.ApplicationUser;
import com.thecodinganalyst.staffalias.security.ApplicationUserRepository;
import com.thecodinganalyst.staffalias.tenant.Tenant;
import com.thecodinganalyst.staffalias.tenant.TenantRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
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
            AccountActivationService activationService) {
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

    public Tenant updateTenant(UUID id, String name) {
        Tenant tenant = getTenant(id);
        tenant.rename(name);
        return tenantRepository.save(tenant);
    }

    public TenantProvisioningResult provisionTenant(String code, String name, String adminEmail) {
        String normalizedEmail = adminEmail.trim().toLowerCase(Locale.ROOT);
        if (tenantRepository.findByCode(code).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tenant code already exists");
        }
        if (userRepository.findByUsernameIgnoreCase(normalizedEmail).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tenant admin email already exists");
        }

        Tenant tenant = tenantRepository.save(new Tenant(code, name));
        ApplicationUser tenantAdmin = new ApplicationUser(
                normalizedEmail, null, ApplicationRole.TENANT_ADMIN, tenant);
        tenantAdmin.setEmail(normalizedEmail);
        tenantAdmin.disable();
        tenantAdmin = userRepository.save(tenantAdmin);

        boolean activationEmailSent = activationService.issue(tenantAdmin, tenant.getName());

        log.info("Platform administration provisioned tenant id={} code={} adminUserId={} activationEmailSent={}",
                tenant.getId(), tenant.getCode(), tenantAdmin.getId(), activationEmailSent);
        return new TenantProvisioningResult(tenant, tenantAdmin, activationEmailSent);
    }

    public record TenantProvisioningResult(Tenant tenant, ApplicationUser tenantAdmin, boolean activationEmailSent) {
    }
}
