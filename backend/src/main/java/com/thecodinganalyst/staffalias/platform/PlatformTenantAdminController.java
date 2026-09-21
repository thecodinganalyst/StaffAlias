package com.thecodinganalyst.staffalias.platform;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.thecodinganalyst.staffalias.platform.PlatformTenantAdminService.TenantProvisioningResult;
import com.thecodinganalyst.staffalias.tenant.Tenant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/platform/tenants")
public class PlatformTenantAdminController {

    private final PlatformTenantAdminService service;

    public PlatformTenantAdminController(PlatformTenantAdminService service) {
        this.service = service;
    }

    @GetMapping
    public List<TenantResponse> list() {
        return service.listTenants().stream().map(TenantResponse::from).toList();
    }

    @GetMapping("/{id}")
    public TenantResponse get(@PathVariable UUID id) {
        return TenantResponse.from(service.getTenant(id));
    }

    @PostMapping
    public ResponseEntity<TenantProvisioningResponse> create(@Valid @RequestBody CreateTenantRequest request) {
        TenantProvisioningResult result = service.provisionTenant(
                request.code().trim(),
                request.name().trim(),
                request.adminEmail().trim());
        Tenant tenant = result.tenant();
        return ResponseEntity.created(URI.create("/api/platform/tenants/" + tenant.getId()))
                .body(TenantProvisioningResponse.from(result));
    }

    public record CreateTenantRequest(
            @NotBlank @Size(max = 64) String code,
            @NotBlank @Size(max = 200) String name,
            @NotBlank @Email @Size(max = 320) String adminEmail) {
    }

    public record TenantResponse(UUID id, String code, String name) {
        static TenantResponse from(Tenant tenant) {
            return new TenantResponse(tenant.getId(), tenant.getCode(), tenant.getName());
        }
    }

    public record TenantAdminResponse(UUID id, String username, String role, UUID tenantId) {
        static TenantAdminResponse from(TenantProvisioningResult result) {
            return new TenantAdminResponse(
                    result.tenantAdmin().getId(),
                    result.tenantAdmin().getUsername(),
                    result.tenantAdmin().getRole().name(),
                    result.tenant().getId());
        }
    }

    public record TenantProvisioningResponse(TenantResponse tenant, TenantAdminResponse tenantAdmin, boolean activationEmailSent) {
        static TenantProvisioningResponse from(TenantProvisioningResult result) {
            return new TenantProvisioningResponse(
                    TenantResponse.from(result.tenant()),
                    TenantAdminResponse.from(result),
                    result.activationEmailSent());
        }
    }
}
