package com.lifeadmin.integration.provider.impl;

import com.lifeadmin.integration.ProviderConnection;
import com.lifeadmin.integration.provider.ConnectedServiceProvider;
import org.springframework.stereotype.Component;

public class UnsupportedProviderStub implements ConnectedServiceProvider {

    // This stub represents all providers that we know of but don't have an API for yet.
    // In a real system, we'd have specific implementations for each.
    // We'll let the ProviderRegistry handle returning this for unknown providers.

    private final String name;

    public UnsupportedProviderStub(String name) {
        this.name = name;
    }

    @Override
    public String getProviderName() {
        return name;
    }

    @Override
    public String getDisplayName() {
        return name;
    }

    @Override
    public String getCategory() {
        return "UNKNOWN";
    }

    @Override
    public String getDescription() {
        return "Unsupported provider.";
    }

    @Override
    public String getOfficialPortalUrl() {
        return null;
    }

    @Override
    public void sync(ProviderConnection connection) {
        throw new UnsupportedOperationException("Automatic connection isn't available for this provider yet.");
    }

    @Override
    public void sync(ProviderConnection connection, java.util.Map<String, String> credentials) {
        throw new UnsupportedOperationException("Automatic connection isn't available for this provider yet.");
    }

    @Override
    public boolean supportsAutomaticSync() {
        return false;
    }
}
