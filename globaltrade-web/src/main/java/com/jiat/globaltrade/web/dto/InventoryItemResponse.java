package com.jiat.globaltrade.web.dto;

import com.jiat.globaltrade.entity.InventoryItem;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Safe, detached REST response DTO for InventoryItem entities.
 */
public class InventoryItemResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String sku;
    private String itemName;
    private int quantity;
    private int reorderLevel;
    private BigDecimal unitPrice;
    private Long warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private LocalDateTime lastUpdated;

    public InventoryItemResponse() {
    }

    public static InventoryItemResponse fromEntity(InventoryItem item) {
        if (item == null) {
            return null;
        }
        InventoryItemResponse dto = new InventoryItemResponse();
        dto.setId(item.getId());
        dto.setSku(item.getSku());
        dto.setItemName(item.getItemName());
        dto.setQuantity(item.getQuantity());
        dto.setReorderLevel(item.getReorderLevel());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setLastUpdated(item.getLastUpdated());
        if (item.getWarehouse() != null) {
            dto.setWarehouseId(item.getWarehouse().getId());
            dto.setWarehouseCode(item.getWarehouse().getWarehouseCode());
            dto.setWarehouseName(item.getWarehouse().getName());
        }
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(int reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getWarehouseCode() {
        return warehouseCode;
    }

    public void setWarehouseCode(String warehouseCode) {
        this.warehouseCode = warehouseCode;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
