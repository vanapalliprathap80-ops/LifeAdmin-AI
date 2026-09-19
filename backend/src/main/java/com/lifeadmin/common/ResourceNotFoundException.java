package com.lifeadmin.common;

import java.util.UUID;

/**
 * Generic resource-not-found exception for entities without their own exception class.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceType, UUID id) {
        super(resourceType + " not found: " + id);
    }
}
