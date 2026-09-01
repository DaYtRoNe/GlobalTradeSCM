package com.jiat.globaltrade.web.dto;

import java.io.Serializable;

public class WarehouseStockResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String sku;
    private String warehouseCode;
    private String binLocation;
    private int physicalOnHand;
    private int allocatedQuantity;
    private int availableToPromise;
    private String replenishmentStatus;
    private String syncTimestamp;
    private String integrationMode;
    private String sourceSystem;

    public WarehouseStockResponse() {
    }

    public WarehouseStockResponse(String sku, String warehouseCode, String binLocation,
                                  int physicalOnHand, int allocatedQuantity, int availableToPromise,
                                  String replenishmentStatus, String syncTimestamp,
                                  String integrationMode, String sourceSystem) {
        this.sku = sku;
        this.warehouseCode = warehouseCode;
        this.binLocation = binLocation;
        this.physicalOnHand = physicalOnHand;
        this.allocatedQuantity = allocatedQuantity;
        this.availableToPromise = availableToPromise;
        this.replenishmentStatus = replenishmentStatus;
        this.syncTimestamp = syncTimestamp;
        this.integrationMode = integrationMode;
        this.sourceSystem = sourceSystem;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getWarehouseCode() {
        return warehouseCode;
    }

    public void setWarehouseCode(String warehouseCode) {
        this.warehouseCode = warehouseCode;
    }

    public String getBinLocation() {
        return binLocation;
    }

    public void setBinLocation(String binLocation) {
        this.binLocation = binLocation;
    }

    public int getPhysicalOnHand() {
        return physicalOnHand;
    }

    public void setPhysicalOnHand(int physicalOnHand) {
        this.physicalOnHand = physicalOnHand;
    }

    public int getAllocatedQuantity() {
        return allocatedQuantity;
    }

    public void setAllocatedQuantity(int allocatedQuantity) {
        this.allocatedQuantity = allocatedQuantity;
    }

    public int getAvailableToPromise() {
        return availableToPromise;
    }

    public void setAvailableToPromise(int availableToPromise) {
        this.availableToPromise = availableToPromise;
    }

    public String getReplenishmentStatus() {
        return replenishmentStatus;
    }

    public void setReplenishmentStatus(String replenishmentStatus) {
        this.replenishmentStatus = replenishmentStatus;
    }

    public String getSyncTimestamp() {
        return syncTimestamp;
    }

    public void setSyncTimestamp(String syncTimestamp) {
        this.syncTimestamp = syncTimestamp;
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
