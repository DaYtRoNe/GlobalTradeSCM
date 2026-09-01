package com.jiat.globaltrade.integration.gateway;

import com.jiat.globaltrade.integration.model.SupplierCatalogPayload;

/**
 * Enterprise Integration Gateway interface for Vendor & Supplier B2B Portals.
 */
public interface SupplierPortalGateway {

    SupplierCatalogPayload querySupplierInfo(String vendorCode);

    boolean transmitPurchaseOrder(String vendorCode, String sku, int quantity, String deliveryTerms);
}
