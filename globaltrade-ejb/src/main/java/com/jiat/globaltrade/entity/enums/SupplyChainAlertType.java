package com.jiat.globaltrade.entity.enums;

/**
 * Standardized alert classifications for automated supply chain monitoring.
 */
public enum SupplyChainAlertType {

    /** Overdue freight consignment past expected delivery date. */
    SHIPMENT_DELAY,

    /** Warehouse inventory item at or below configured reorder threshold. */
    INVENTORY_REPLENISHMENT_REQUIRED,

    /** Supplier with performance rating below minimum acceptable quality score. */
    VENDOR_PERFORMANCE_RISK,

    /** Statutory customs declaration approaching or past submission deadline. */
    CUSTOMS_DOCUMENT_DEADLINE
}
