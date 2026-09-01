package com.jiat.globaltrade.web.dto;

import com.jiat.globaltrade.entity.RouteOptimizationRecommendation;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * Public REST DTO representing a persistent route optimization recommendation.
 */
public class RouteOptimizationResponse implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    private Long id;
    private Long shipmentId;
    private String trackingNumber;
    private String origin;
    private String destination;
    private Long selectedRouteId;
    private String selectedRouteCode;
    private String carrierName;
    private String transportMode;
    private BigDecimal optimizationScore;
    private Integer transitTimeHours;
    private BigDecimal estimatedCost;
    private BigDecimal riskScore;
    private String evaluatedAt;
    private String evaluationSource;
    private String summaryRationale;

    public RouteOptimizationResponse() {
    }

    public static RouteOptimizationResponse fromEntity(RouteOptimizationRecommendation rec) {
        if (rec == null) {
            return null;
        }

        RouteOptimizationResponse dto = new RouteOptimizationResponse();
        dto.setId(rec.getId());

        if (rec.getShipment() != null) {
            dto.setShipmentId(rec.getShipment().getId());
            dto.setTrackingNumber(rec.getShipment().getTrackingNumber());
            dto.setOrigin(rec.getShipment().getOrigin());
            dto.setDestination(rec.getShipment().getDestination());
        }

        if (rec.getSelectedRoute() != null) {
            dto.setSelectedRouteId(rec.getSelectedRoute().getId());
            dto.setSelectedRouteCode(rec.getSelectedRoute().getRouteCode());
            dto.setCarrierName(rec.getSelectedRoute().getCarrierName());
            dto.setTransportMode(rec.getSelectedRoute().getTransportMode());
        }

        dto.setOptimizationScore(rec.getOptimizationScore());
        dto.setTransitTimeHours(rec.getTransitTimeHours());
        dto.setEstimatedCost(rec.getEstimatedCost());
        dto.setRiskScore(rec.getRiskScore());

        if (rec.getEvaluatedAt() != null) {
            dto.setEvaluatedAt(rec.getEvaluatedAt().format(FORMATTER));
        }

        dto.setEvaluationSource(rec.getEvaluationSource());
        dto.setSummaryRationale(rec.getSummaryRationale());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getShipmentId() {
        return shipmentId;
    }

    public void setShipmentId(Long shipmentId) {
        this.shipmentId = shipmentId;
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

    public Long getSelectedRouteId() {
        return selectedRouteId;
    }

    public void setSelectedRouteId(Long selectedRouteId) {
        this.selectedRouteId = selectedRouteId;
    }

    public String getSelectedRouteCode() {
        return selectedRouteCode;
    }

    public void setSelectedRouteCode(String selectedRouteCode) {
        this.selectedRouteCode = selectedRouteCode;
    }

    public String getCarrierName() {
        return carrierName;
    }

    public void setCarrierName(String carrierName) {
        this.carrierName = carrierName;
    }

    public String getTransportMode() {
        return transportMode;
    }

    public void setTransportMode(String transportMode) {
        this.transportMode = transportMode;
    }

    public BigDecimal getOptimizationScore() {
        return optimizationScore;
    }

    public void setOptimizationScore(BigDecimal optimizationScore) {
        this.optimizationScore = optimizationScore;
    }

    public Integer getTransitTimeHours() {
        return transitTimeHours;
    }

    public void setTransitTimeHours(Integer transitTimeHours) {
        this.transitTimeHours = transitTimeHours;
    }

    public BigDecimal getEstimatedCost() {
        return estimatedCost;
    }

    public void setEstimatedCost(BigDecimal estimatedCost) {
        this.estimatedCost = estimatedCost;
    }

    public BigDecimal getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(BigDecimal riskScore) {
        this.riskScore = riskScore;
    }

    public String getEvaluatedAt() {
        return evaluatedAt;
    }

    public void setEvaluatedAt(String evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
    }

    public String getEvaluationSource() {
        return evaluationSource;
    }

    public void setEvaluationSource(String evaluationSource) {
        this.evaluationSource = evaluationSource;
    }

    public String getSummaryRationale() {
        return summaryRationale;
    }

    public void setSummaryRationale(String summaryRationale) {
        this.summaryRationale = summaryRationale;
    }
}
