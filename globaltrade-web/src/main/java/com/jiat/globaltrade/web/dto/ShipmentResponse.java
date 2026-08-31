package com.jiat.globaltrade.web.dto;

import com.jiat.globaltrade.entity.Shipment;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Safe, detached REST response DTO for Shipment entities.
 */
public class ShipmentResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String trackingNumber;
    private String origin;
    private String destination;
    private String shipmentStatus;
    private LocalDate expectedDeliveryDate;
    private LocalDate actualDeliveryDate;
    private Long vendorId;
    private String vendorName;
    private String customerUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ShipmentResponse() {
    }

    public static ShipmentResponse fromEntity(Shipment shipment) {
        if (shipment == null) {
            return null;
        }
        ShipmentResponse dto = new ShipmentResponse();
        dto.setId(shipment.getId());
        dto.setTrackingNumber(shipment.getTrackingNumber());
        dto.setOrigin(shipment.getOrigin());
        dto.setDestination(shipment.getDestination());
        dto.setShipmentStatus(shipment.getShipmentStatus() != null ? shipment.getShipmentStatus().name() : null);
        dto.setExpectedDeliveryDate(shipment.getExpectedDeliveryDate());
        dto.setActualDeliveryDate(shipment.getActualDeliveryDate());
        dto.setCustomerUsername(shipment.getCustomerUsername());
        dto.setCreatedAt(shipment.getCreatedAt());
        dto.setUpdatedAt(shipment.getUpdatedAt());
        if (shipment.getVendor() != null) {
            dto.setVendorId(shipment.getVendor().getId());
            dto.setVendorName(shipment.getVendor().getCompanyName());
        }
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getShipmentStatus() {
        return shipmentStatus;
    }

    public void setShipmentStatus(String shipmentStatus) {
        this.shipmentStatus = shipmentStatus;
    }

    public LocalDate getExpectedDeliveryDate() {
        return expectedDeliveryDate;
    }

    public void setExpectedDeliveryDate(LocalDate expectedDeliveryDate) {
        this.expectedDeliveryDate = expectedDeliveryDate;
    }

    public LocalDate getActualDeliveryDate() {
        return actualDeliveryDate;
    }

    public void setActualDeliveryDate(LocalDate actualDeliveryDate) {
        this.actualDeliveryDate = actualDeliveryDate;
    }

    public Long getVendorId() {
        return vendorId;
    }

    public void setVendorId(Long vendorId) {
        this.vendorId = vendorId;
    }

    public String getVendorName() {
        return vendorName;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    public String getCustomerUsername() {
        return customerUsername;
    }

    public void setCustomerUsername(String customerUsername) {
        this.customerUsername = customerUsername;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
