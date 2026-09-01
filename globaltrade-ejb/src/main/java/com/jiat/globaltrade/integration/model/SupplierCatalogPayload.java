package com.jiat.globaltrade.integration.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payload representing Vendor B2B Supplier Portal live inventory and lead times.
 */
public record SupplierCatalogPayload(
        String vendorCode,
        String companyName,
        String supplierStatus,
        int leadTimeDays,
        BigDecimal minimumOrderValueUsd,
        boolean acceptElectronicPurchaseOrders,
        LocalDateTime lastCatalogUpdate,
        String integrationMode,
        String sourceSystem
) implements Serializable {}
