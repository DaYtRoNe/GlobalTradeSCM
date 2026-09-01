package com.jiat.globaltrade.integration.adapter;

import com.jiat.globaltrade.integration.gateway.WarehouseManagementGateway;
import com.jiat.globaltrade.integration.model.WarehouseStockPayload;
import jakarta.ejb.Stateless;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Enterprise Simulated Adapter for Warehouse Management Systems (WMS).
 * Demonstrates real-time bin storage tracking, physical counts, and replenishment dispatch.
 */
@Stateless
public class SimulatedWarehouseGatewayBean implements WarehouseManagementGateway, Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(SimulatedWarehouseGatewayBean.class.getName());

    public static final String INTEGRATION_MODE = "SIMULATED";
    public static final String SOURCE_SYSTEM = "SIMULATED_WMS_GATEWAY_V1";

    @Override
    public WarehouseStockPayload queryBinStock(String sku) {
        LOGGER.log(Level.INFO, "[SimulatedWarehouseGateway] Interrogating WMS storage rack for SKU: {0}", sku);

        String binLocation = "RACK-A4-BIN-12";
        int onHand = 1200;
        int allocated = 150;
        int atp = 1050;
        String status = "STOCK_NOMINAL";

        return new WarehouseStockPayload(
                sku,
                "WH-SIN-01",
                binLocation,
                onHand,
                allocated,
                atp,
                status,
                LocalDateTime.now(),
                INTEGRATION_MODE,
                SOURCE_SYSTEM
        );
    }

    @Override
    public boolean requestReplenishmentOrder(String sku, int requestedQuantity, String targetWarehouseCode) {
        LOGGER.log(Level.INFO, "[SimulatedWarehouseGateway] Generated internal WMS replenishment transfer: SKU={0}, Qty={1}, Destination={2}",
                new Object[]{sku, requestedQuantity, targetWarehouseCode});
        return true;
    }
}
