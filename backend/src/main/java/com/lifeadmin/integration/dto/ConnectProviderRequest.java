package com.lifeadmin.integration.dto;

import lombok.Data;

import java.util.Map;

@Data
public class ConnectProviderRequest {
    private String providerName;
    private String authCode; // e.g. OAuth code for the stub
    private Map<String, String> credentials;
}
