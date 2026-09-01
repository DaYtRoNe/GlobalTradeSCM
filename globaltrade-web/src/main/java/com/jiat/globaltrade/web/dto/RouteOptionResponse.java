package com.jiat.globaltrade.web.dto;

import com.jiat.globaltrade.entity.RouteOption;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Public REST DTO representing a candidate transport corridor option.
 */
public class RouteOptionResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String routeCode;
    private String origin;
    private String destination;
    private String carrierName;
    private String carrierCode;
    private String transportMode;
    private Integer estimatedTransitHours;
    private BigDecimal estimatedCost;
    private BigDecimal operationalRiskScore;
    private Boolean active;

    public RouteOptionResponse() {
    }

    public static RouteOptionResponse fromEntity(RouteOption route) {
        if (route == null) {
            return null;
        }

        RouteOptionResponse dto = new RouteOptionResponse();
        dto.setId(route.getId());
        dto.setRouteCode(route.getRouteCode());
        dto.setOrigin(route.getOrigin());
        dto.setDestination(route.getDestination());
        dto.setCarrierName(route.getCarrierName());
        dto.setCarrierCode(route.getCarrierCode());
        dto.setTransportMode(route.getTransportMode());
        dto.setEstimatedTransitHours(route.getEstimatedTransitHours());
        dto.setEstimatedCost(route.getEstimatedCost());
        dto.setOperationalRiskScore(route.getOperationalRiskScore());
        dto.setActive(route.getActive());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRouteCode() {
        return routeCode;
    }

    public void setRouteCode(String routeCode) {
        this.routeCode = routeCode;
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

    public Integer getEstimatedTransitHours() {
        return estimatedTransitHours;
    }

    public void setEstimatedTransitHours(Integer estimatedTransitHours) {
        this.estimatedTransitHours = estimatedTransitHours;
    }

    public BigDecimal getEstimatedCost() {
        return estimatedCost;
    }

    public void setEstimatedCost(BigDecimal estimatedCost) {
        this.estimatedCost = estimatedCost;
    }

    public BigDecimal getOperationalRiskScore() {
        return operationalRiskScore;
    }

    public void setOperationalRiskScore(BigDecimal operationalRiskScore) {
        this.operationalRiskScore = operationalRiskScore;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
