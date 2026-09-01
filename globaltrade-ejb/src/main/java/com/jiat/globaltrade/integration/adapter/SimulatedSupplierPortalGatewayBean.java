package com.jiat.globaltrade.integration.adapter;

import com.jiat.globaltrade.integration.gateway.SupplierPortalGateway;
import com.jiat.globaltrade.integration.model.SupplierCatalogPayload;
import jakarta.ejb.Stateless;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Enterprise Simulated Adapter for Vendor B2B Supplier Portal Integration.
 * Demonstrates electronic purchase order dispatch and supplier lead time synchronization.
 */
@Stateless
public class SimulatedSupplierPortalGatewayBean implements SupplierPortalGateway, Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(SimulatedSupplierPortalGatewayBean.class.getName());

    public static final String INTEGRATION_MODE = "SIMULATED";
    public static final String SOURCE_SYSTEM = "SIMULATED_SUPPLIER_B2B_PORTAL_V1";

    @Override
    public SupplierCatalogPayload querySupplierInfo(String vendorCode) {
        LOGGER.log(Level.INFO, "[SimulatedSupplierPortalGateway] Querying supplier portal metrics for: {0}", vendorCode);

        String company = "VND-001".equals(vendorCode) ? "Pacific Cargo Ltd" : "Global Preferred Supplier";
        String status = "VERIFIED_SUPPLIER_ACTIVE";
        int leadTime = 7;
        BigDecimal mov = new BigDecimal("1000.00");

        return new SupplierCatalogPayload(
                vendorCode,
                company,
                status,
                leadTime,
                mov,
                true,
                LocalDateTime.now(),
                INTEGRATION_MODE,
                SOURCE_SYSTEM
        );
    }

    @Override
    public boolean transmitPurchaseOrder(String vendorCode, String sku, int quantity, String deliveryTerms) {
        LOGGER.log(Level.INFO, "[SimulatedSupplierPortalGateway] Transmitted B2B Electronic Purchase Order to {0}: SKU={1}, Qty={2}, Terms={3}",
                new Object[]{vendorCode, sku, quantity, deliveryTerms});
        return true;
    }
}
