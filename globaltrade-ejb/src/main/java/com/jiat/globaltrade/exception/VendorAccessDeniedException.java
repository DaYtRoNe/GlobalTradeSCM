package com.jiat.globaltrade.exception;

import jakarta.ejb.ApplicationException;

/**
 * Application exception thrown when an authenticated caller is denied fine-grained access
 * to a specific vendor record (e.g. cross-vendor access violation attempt).
 *
 * Configured with @ApplicationException(rollback = false) because this represents an expected
 * security authorization decision, not an unexpected system failure requiring transaction rollback.
 */
@ApplicationException(rollback = false)
public class VendorAccessDeniedException extends Exception {

    private static final long serialVersionUID = 1L;

    private final Long vendorId;
    private final String username;

    public VendorAccessDeniedException(Long vendorId, String username) {
        super(String.format("Caller '%s' is not authorized to access Vendor ID %d", username, vendorId));
        this.vendorId = vendorId;
        this.username = username;
    }

    public VendorAccessDeniedException(String message) {
        super(message);
        this.vendorId = null;
        this.username = null;
    }

    public Long getVendorId() {
        return vendorId;
    }

    public String getUsername() {
        return username;
    }
}
