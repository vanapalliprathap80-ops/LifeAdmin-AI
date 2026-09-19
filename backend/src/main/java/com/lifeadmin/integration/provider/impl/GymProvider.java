package com.lifeadmin.integration.provider.impl;
import com.lifeadmin.integration.ProviderConnection;
import com.lifeadmin.integration.provider.ConnectedServiceProvider;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class GymProvider implements ConnectedServiceProvider {
    @Override public String getProviderName() { return "anytime-fitness"; }
    @Override public String getDisplayName() { return "Anytime Fitness"; }
    @Override public String getCategory() { return "Health & Fitness"; }
    @Override public String getDescription() { return "Track your gym membership, renewal dates, and class packages."; }
    @Override public String getOfficialPortalUrl() { return "https://anytimefitness.com"; }
    @Override public void sync(ProviderConnection connection) {}
    @Override public void sync(ProviderConnection connection, Map<String, String> credentials) {}
    @Override public boolean supportsAutomaticSync() { return false; }
}
