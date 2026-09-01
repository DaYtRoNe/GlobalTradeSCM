package com.jiat.globaltrade.web.dto;

import java.io.Serializable;

public class CarrierTrackingResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String trackingNumber;
    private String carrierName;
    private String carrierCode;
    private String transportMode;
    private String externalStatusCode;
    private String currentCheckpoint;
    private String estimatedDeliveryWindow;
    private String lastEventTimestamp;
    private String integrationMode;
    private String sourceSystem;

    public CarrierTrackingResponse() {
    }

    public CarrierTrackingResponse(String trackingNumber, String carrierName, String carrierCode,
                                   String transportMode, String externalStatusCode,
                                   String currentCheckpoint, String estimatedDeliveryWindow,
                                   String lastEventTimestamp, String integrationMode,
                                   String sourceSystem) {
        this.trackingNumber = trackingNumber;
        this.carrierName = carrierName;
        this.carrierCode = carrierCode;
        this.transportMode = transportMode;
        this.externalStatusCode = externalStatusCode;
        this.currentCheckpoint = currentCheckpoint;
        this.estimatedDeliveryWindow = estimatedDeliveryWindow;
        this.lastEventTimestamp = lastEventTimestamp;
        this.integrationMode = integrationMode;
        this.sourceSystem = sourceSystem;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public String getCarrierName() {
        return carrierName;
    }

    public void setCarrierName(String carrierName) {
        this.carrierName = carrierName;
    }

    public String getCarrierCode() {
        return carrierCode;
    }

    public void setCarrierCode(String carrierCode) {
        this.carrierCode = carrierCode;
    }

    public String getTransportMode() {
        return transportMode;
    }

    public void setTransportMode(String transportMode) {
        this.transportMode = transportMode;
    }

    public String getExternalStatusCode() {
        return externalStatusCode;
    }

    public void setExternalStatusCode(String externalStatusCode) {
        this.externalStatusCode = externalStatusCode;
    }

    public String getCurrentCheckpoint() {
        return currentCheckpoint;
    }

    public void setCurrentCheckpoint(String currentCheckpoint) {
        this.currentCheckpoint = currentCheckpoint;
    }

    public String getEstimatedDeliveryWindow() {
        return estimatedDeliveryWindow;
    }

    public void setEstimatedDeliveryWindow(String estimatedDeliveryWindow) {
        this.estimatedDeliveryWindow = estimatedDeliveryWindow;
    }

    public String getLastEventTimestamp() {
        return lastEventTimestamp;
    }

    public void setLastEventTimestamp(String lastEventTimestamp) {
        this.lastEventTimestamp = lastEventTimestamp;
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
