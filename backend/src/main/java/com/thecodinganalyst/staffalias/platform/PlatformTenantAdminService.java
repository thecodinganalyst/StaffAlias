package com.thecodinganalyst.staffalias.platform;

import java.util.List;
import java.util.UUID;

import com.thecodinganalyst.staffalias.tenant.Tenant;
import com.thecodinganalyst.staffalias.tenant.TenantRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PlatformTenantAdminService {

    private static final Logger log = LoggerFactory.getLogger(PlatformTenantAdminService.class);
    private final TenantRepository tenantRepository;

    public PlatformTenantAdminService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
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

    public Tenant createTenant(String code, String name) {
        if (tenantRepository.findByCode(code).isPresent()) {
            throw new DataIntegrityViolationException("Tenant code already exists");
        }
        Tenant tenant = tenantRepository.save(new Tenant(code, name));
        log.info("Platform administration created tenant id={} code={}", tenant.getId(), tenant.getCode());
        return tenant;
    }
}
