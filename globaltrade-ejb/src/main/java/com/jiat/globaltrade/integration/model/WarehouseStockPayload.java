package com.jiat.globaltrade.integration.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Payload representing Warehouse Management System (WMS) live bin allocation and stock count.
 */
public record WarehouseStockPayload(
        String sku,
        String warehouseCode,
        String binLocation,
        int physicalOnHand,
        int allocatedQuantity,
        int availableToPromise,
        String replenishmentStatus,
        LocalDateTime syncTimestamp,
        String integrationMode,
        String sourceSystem
) implements Serializable {}
