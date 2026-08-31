package com.jiat.globaltrade.exception;

import jakarta.ejb.ApplicationException;

/**
 * Application exception thrown when a requested domain entity (Vendor, Shipment, InventoryItem, etc.)
 * cannot be located by its identifier.
 *
 * Configured with @ApplicationException(rollback = false) because a non-existent entity lookup
 * is an expected informational result and does not require rolling back the transactional context.
 */
@ApplicationException(rollback = false)
public class ResourceNotFoundException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String resourceType;
    private final Object resourceId;

    public ResourceNotFoundException(String resourceType, Object resourceId) {
        super(String.format("%s with identifier '%s' was not found.", resourceType, resourceId));
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    public ResourceNotFoundException(String message) {
        super(message);
        this.resourceType = "Resource";
        this.resourceId = null;
    }

    public String getResourceType() {
        return resourceType;
    }

    public Object getResourceId() {
        return resourceId;
    }
}
