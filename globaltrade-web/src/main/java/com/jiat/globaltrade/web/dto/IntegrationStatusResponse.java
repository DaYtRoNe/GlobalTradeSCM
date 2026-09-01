package com.jiat.globaltrade.web.dto;

import java.io.Serializable;
import java.util.Map;

public class IntegrationStatusResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String overallStatus;
    private String adapterEnvironment;
    private String timestamp;
    private Map<String, String> gatewayStatusMap;
    private int activeGateways;
    private int degradedGateways;

    public IntegrationStatusResponse() {
    }

    public IntegrationStatusResponse(String overallStatus, String adapterEnvironment,
                                     String timestamp, Map<String, String> gatewayStatusMap,
                                     int activeGateways, int degradedGateways) {
        this.overallStatus = overallStatus;
        this.adapterEnvironment = adapterEnvironment;
        this.timestamp = timestamp;
        this.gatewayStatusMap = gatewayStatusMap;
        this.activeGateways = activeGateways;
        this.degradedGateways = degradedGateways;
    }

    public String getOverallStatus() {
        return overallStatus;
    }

    public void setOverallStatus(String overallStatus) {
        this.overallStatus = overallStatus;
    }

    public String getAdapterEnvironment() {
        return adapterEnvironment;
    }

    public void setAdapterEnvironment(String adapterEnvironment) {
        this.adapterEnvironment = adapterEnvironment;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, String> getGatewayStatusMap() {
        return gatewayStatusMap;
    }

    public void setGatewayStatusMap(Map<String, String> gatewayStatusMap) {
        this.gatewayStatusMap = gatewayStatusMap;
    }

    public int getActiveGateways() {
        return activeGateways;
    }

    public void setActiveGateways(int activeGateways) {
        this.activeGateways = activeGateways;
    }

    public int getDegradedGateways() {
        return degradedGateways;
    }

    public void setDegradedGateways(int degradedGateways) {
        this.degradedGateways = degradedGateways;
    }
}
