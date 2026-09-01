package com.jiat.globaltrade.web.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public class SupplierOrderResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String vendorCode;
    private String companyName;
    private String supplierStatus;
    private int leadTimeDays;
    private BigDecimal minimumOrderValueUsd;
    private boolean acceptElectronicPurchaseOrders;
    private String lastCatalogUpdate;
    private String integrationMode;
    private String sourceSystem;

    public SupplierOrderResponse() {
    }

    public SupplierOrderResponse(String vendorCode, String companyName, String supplierStatus,
                                 int leadTimeDays, BigDecimal minimumOrderValueUsd,
                                 boolean acceptElectronicPurchaseOrders, String lastCatalogUpdate,
                                 String integrationMode, String sourceSystem) {
        this.vendorCode = vendorCode;
        this.companyName = companyName;
        this.supplierStatus = supplierStatus;
        this.leadTimeDays = leadTimeDays;
        this.minimumOrderValueUsd = minimumOrderValueUsd;
        this.acceptElectronicPurchaseOrders = acceptElectronicPurchaseOrders;
        this.lastCatalogUpdate = lastCatalogUpdate;
        this.integrationMode = integrationMode;
        this.sourceSystem = sourceSystem;
    }

    public String getVendorCode() {
        return vendorCode;
    }

    public void setVendorCode(String vendorCode) {
        this.vendorCode = vendorCode;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getSupplierStatus() {
        return supplierStatus;
    }

    public void setSupplierStatus(String supplierStatus) {
        this.supplierStatus = supplierStatus;
    }

    public int getLeadTimeDays() {
        return leadTimeDays;
    }

    public void setLeadTimeDays(int leadTimeDays) {
        this.leadTimeDays = leadTimeDays;
    }

    public BigDecimal getMinimumOrderValueUsd() {
        return minimumOrderValueUsd;
    }

    public void setMinimumOrderValueUsd(BigDecimal minimumOrderValueUsd) {
        this.minimumOrderValueUsd = minimumOrderValueUsd;
    }

    public boolean isAcceptElectronicPurchaseOrders() {
        return acceptElectronicPurchaseOrders;
    }

    public void setAcceptElectronicPurchaseOrders(boolean acceptElectronicPurchaseOrders) {
        this.acceptElectronicPurchaseOrders = acceptElectronicPurchaseOrders;
    }

    public String getLastCatalogUpdate() {
        return lastCatalogUpdate;
    }

    public void setLastCatalogUpdate(String lastCatalogUpdate) {
        this.lastCatalogUpdate = lastCatalogUpdate;
    }

    public String getIntegrationMode() {
        return integrationMode;
    }

    public void setIntegrationMode(String integrationMode) {
        this.integrationMode = integrationMode;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }
}
