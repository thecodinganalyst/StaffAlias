package com.thecodinganalyst.staffalias.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/activation")
public class AccountActivationController {
    private final AccountActivationService service;
    public AccountActivationController(AccountActivationService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<Void> activate(@Valid @RequestBody ActivationRequest request) {
        service.activate(request.token(), request.password());
        return ResponseEntity.noContent().build();
    }

    public record ActivationRequest(@NotBlank String token, @NotBlank @Size(min=12, max=200) String password) {}
}
