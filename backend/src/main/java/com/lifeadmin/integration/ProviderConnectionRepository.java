package com.lifeadmin.integration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProviderConnectionRepository extends JpaRepository<ProviderConnection, UUID> {
    
    List<ProviderConnection> findAllByUserId(UUID userId);
    
    Optional<ProviderConnection> findByUserIdAndProviderName(UUID userId, String providerName);
}
