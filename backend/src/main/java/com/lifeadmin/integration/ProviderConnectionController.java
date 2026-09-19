package com.lifeadmin.integration;

import com.lifeadmin.integration.dto.ConnectProviderRequest;
import com.lifeadmin.integration.dto.ProviderConnectionDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import com.lifeadmin.auth.User;
import com.lifeadmin.auth.UserRepository;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/integrations")
@CrossOrigin(origins = "*") // In production, restrict to frontend domain
public class ProviderConnectionController {

    private final ProviderConnectionService providerConnectionService;
    private final UserRepository userRepository;
    private final com.lifeadmin.integration.provider.ProviderRegistry providerRegistry;

    public ProviderConnectionController(ProviderConnectionService providerConnectionService, UserRepository userRepository, com.lifeadmin.integration.provider.ProviderRegistry providerRegistry) {
        this.providerConnectionService = providerConnectionService;
        this.userRepository = userRepository;
        this.providerRegistry = providerRegistry;
    }

    @GetMapping
    public ResponseEntity<List<ProviderConnectionDto>> getConnections(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = getUserId(userDetails);
        return ResponseEntity.ok(providerConnectionService.getUserConnections(userId));
    }

    @GetMapping("/catalog")
    public ResponseEntity<List<com.lifeadmin.integration.dto.ProviderCatalogDto>> getCatalog() {
        var catalog = providerRegistry.getAllProviders().stream().map(p -> {
            var dto = new com.lifeadmin.integration.dto.ProviderCatalogDto();
            dto.setId(p.getProviderName());
            dto.setDisplayName(p.getDisplayName());
            dto.setCategory(p.getCategory());
            dto.setDescription(p.getDescription());
            dto.setOfficialPortalUrl(p.getOfficialPortalUrl());
            return dto;
        }).toList();
        return ResponseEntity.ok(catalog);
    }

    @PostMapping("/connect")
    public ResponseEntity<ProviderConnectionDto> connectProvider(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody ConnectProviderRequest request) {
        
        UUID userId = getUserId(userDetails);
        ProviderConnectionDto dto = providerConnectionService.connect(userId, request.getProviderName(), request.getAuthCode(), request.getCredentials());
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{id}/sync")
    public ResponseEntity<ProviderConnectionDto> syncProvider(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {
        
        UUID userId = getUserId(userDetails);
        ProviderConnectionDto dto = providerConnectionService.reSync(userId, id);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> disconnectProvider(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {
        
        UUID userId = getUserId(userDetails);
        providerConnectionService.disconnect(userId, id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<java.util.Map<String, String>> handleUnsupported(UnsupportedOperationException e) {
        return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
    }

    private UUID getUserId(UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getId();
    }
}
