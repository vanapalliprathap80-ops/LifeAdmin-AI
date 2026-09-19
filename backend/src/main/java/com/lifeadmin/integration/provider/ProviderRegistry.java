package com.lifeadmin.integration.provider;

import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ProviderRegistry {

    private final Map<String, ConnectedServiceProvider> providers;

    public ProviderRegistry(List<ConnectedServiceProvider> providerList) {
        this.providers = providerList.stream()
                .collect(Collectors.toMap(p -> p.getProviderName().toUpperCase(), Function.identity()));
    }

    public ConnectedServiceProvider getProvider(String providerName) {
        ConnectedServiceProvider provider = providers.get(providerName.toUpperCase());
        if (provider == null) {
            return new com.lifeadmin.integration.provider.impl.UnsupportedProviderStub(providerName.toUpperCase());
        }
        return provider;
    }

    public List<ConnectedServiceProvider> getAllProviders() {
        return providers.values().stream()
                .filter(p -> p.supportsAutomaticSync())
                .collect(Collectors.toList());
    }
}
