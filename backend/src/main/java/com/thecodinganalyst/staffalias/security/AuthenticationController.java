package com.thecodinganalyst.staffalias.security;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    @GetMapping("/me")
    public AuthenticatedUserResponse me(@AuthenticationPrincipal StaffAliasPrincipal principal) {
        return new AuthenticatedUserResponse(principal.userId(), principal.username(), principal.role(), principal.tenantId());
    }

    public record AuthenticatedUserResponse(UUID userId, String username, ApplicationRole role, UUID tenantId) {
    }
}
