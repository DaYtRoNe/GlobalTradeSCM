package com.jiat.globaltrade.exception;

import jakarta.ejb.ApplicationException;

/**
 * Application exception thrown when an authenticated caller (such as a customer)
 * attempts to access a shipment they do not own, or customs data for an unowned shipment.
 *
 * Configured with @ApplicationException(rollback = false) because this represents a deliberate
 * fine-grained security authorization decision, not a transactional failure.
 */
@ApplicationException(rollback = false)
public class ShipmentAccessDeniedException extends Exception {

    private static final long serialVersionUID = 1L;

    private final Long shipmentId;
    private final String username;

    public ShipmentAccessDeniedException(Long shipmentId, String username) {
        super(String.format("Caller '%s' is not authorized to access Shipment ID %d", username, shipmentId));
        this.shipmentId = shipmentId;
        this.username = username;
    }

    public ShipmentAccessDeniedException(String message) {
        super(message);
        this.shipmentId = null;
        this.username = null;
    }

    public Long getShipmentId() {
        return shipmentId;
    }

    public String getUsername() {
        return username;
    }
}
