package com.lifeadmin.integration.provider;

import com.lifeadmin.integration.ProviderConnection;

public interface ConnectedServiceProvider {
    
    /**
     * @return the unique provider enum-like string, e.g., "ELECTRICITY_BOARD", "FLIGHT_SYNC"
     */
    String getProviderName();

    /**
     * @return the UI display name, e.g. "State Electricity Board"
     */
    String getDisplayName();

    /**
     * @return the category of this provider
     */
    String getCategory();

    /**
     * @return description for the UI
     */
    String getDescription();

    /**
     * @return official login portal
     */
    String getOfficialPortalUrl();

    /**
     * Simulates fetching external data and returning a normalized list of assets/subscriptions
     * Note: Depending on the architecture, this might just sync directly to the database.
     * For this interface, we'll let it perform the sync operations directly.
     */
    void sync(ProviderConnection connection);

    /**
     * Sync with specific credentials (e.g. Account Number, PNR)
     */
    void sync(ProviderConnection connection, java.util.Map<String, String> credentials);
    
    /**
     * Checks if this provider actually supports automated synchronization.
     * @return true if supported, false if it's just a fallback stub.
     */
    boolean supportsAutomaticSync();
}
