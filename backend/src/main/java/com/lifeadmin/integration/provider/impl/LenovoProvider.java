package com.lifeadmin.integration.provider.impl;
import com.lifeadmin.integration.ProviderConnection;
import com.lifeadmin.integration.provider.ConnectedServiceProvider;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class LenovoProvider implements ConnectedServiceProvider {
    @Override public String getProviderName() { return "lenovo-vantage"; }
    @Override public String getDisplayName() { return "Lenovo Vantage"; }
    @Override public String getCategory() { return "Technology"; }
    @Override public String getDescription() { return "Connect your Lenovo account to track device warranties, support plans, and hardware maintenance schedules."; }
    @Override public String getOfficialPortalUrl() { return "https://lenovo.com"; }
    @Override public void sync(ProviderConnection connection) {}
    @Override public void sync(ProviderConnection connection, Map<String, String> credentials) {}
    @Override public boolean supportsAutomaticSync() { return false; }
}
