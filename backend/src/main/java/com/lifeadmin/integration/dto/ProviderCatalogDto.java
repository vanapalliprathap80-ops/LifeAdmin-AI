package com.lifeadmin.integration.dto;

import lombok.Data;

@Data
public class ProviderCatalogDto {
    private String id;
    private String displayName;
    private String category;
    private String description;
    private String officialPortalUrl;
}
