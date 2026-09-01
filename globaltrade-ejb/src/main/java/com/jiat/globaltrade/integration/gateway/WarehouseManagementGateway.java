package com.jiat.globaltrade.integration.gateway;

import com.jiat.globaltrade.integration.model.WarehouseStockPayload;

/**
 * Enterprise Integration Gateway interface for Warehouse Management Systems (WMS).
 */
public interface WarehouseManagementGateway {

    WarehouseStockPayload queryBinStock(String sku);

    boolean requestReplenishmentOrder(String sku, int requestedQuantity, String targetWarehouseCode);
}
