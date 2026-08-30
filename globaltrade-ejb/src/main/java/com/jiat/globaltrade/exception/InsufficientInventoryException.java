package com.jiat.globaltrade.exception;

import jakarta.ejb.ApplicationException;

/**
 * Business exception thrown when an inventory operation requested exceeds the available stock.
 * Annotated with @ApplicationException(rollback = true) to instruct the EJB container
 * to automatically mark the current container-managed transaction for rollback.
 */
@ApplicationException(rollback = true)
public class InsufficientInventoryException extends Exception {

    private static final long serialVersionUID = 1L;

    private final Long itemId;
    private final int requestedQuantity;
    private final int availableQuantity;

    public InsufficientInventoryException(Long itemId, int requestedQuantity, int availableQuantity) {
        super(String.format("Insufficient inventory for item ID %d. Requested: %d, Available: %d",
                itemId, requestedQuantity, availableQuantity));
        this.itemId = itemId;
        this.requestedQuantity = requestedQuantity;
        this.availableQuantity = availableQuantity;
    }

    public InsufficientInventoryException(String message) {
        super(message);
        this.itemId = null;
        this.requestedQuantity = 0;
        this.availableQuantity = 0;
    }

    public Long getItemId() {
        return itemId;
    }

    public int getRequestedQuantity() {
        return requestedQuantity;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }
}
