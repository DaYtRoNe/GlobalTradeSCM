package com.jiat.globaltrade.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity representing a candidate transport route corridor offered by freight carriers.
 */
@Entity
@Table(name = "route_options")
public class RouteOption implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "route_code", nullable = false, unique = true, length = 50)
    private String routeCode;

    @Column(name = "origin", nullable = false, length = 100)
    private String origin;

    @Column(name = "destination", nullable = false, length = 100)
    private String destination;

    @Column(name = "carrier_name", nullable = false, length = 100)
    private String carrierName;

    @Column(name = "carrier_code", length = 50)
    private String carrierCode;

    @Column(name = "transport_mode", nullable = false, length = 30)
    private String transportMode;

    @Column(name = "estimated_transit_hours", nullable = false)
    private Integer estimatedTransitHours;

    @Column(name = "estimated_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal estimatedCost;

    @Column(name = "operational_risk_score", nullable = false, precision = 3, scale = 2)
    private BigDecimal operationalRiskScore;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public RouteOption() {
    }

    public RouteOption(String routeCode, String origin, String destination, String carrierName,
                       String carrierCode, String transportMode, Integer estimatedTransitHours,
                       BigDecimal estimatedCost, BigDecimal operationalRiskScore, Boolean active) {
        this.routeCode = routeCode;
        this.origin = origin;
        this.destination = destination;
        this.carrierName = carrierName;
        this.carrierCode = carrierCode;
        this.transportMode = transportMode;
        this.estimatedTransitHours = estimatedTransitHours;
        this.estimatedCost = estimatedCost;
        this.operationalRiskScore = operationalRiskScore;
        this.active = active != null ? active : true;
    }

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.active == null) {
            this.active = true;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RouteOption that = (RouteOption) o;
        return Objects.equals(routeCode, that.routeCode);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(routeCode);
    }

    @Override
    public String toString() {
        return "RouteOption{" +
                "id=" + id +
                ", routeCode='" + routeCode + '\'' +
                ", origin='" + origin + '\'' +
                ", destination='" + destination + '\'' +
                ", carrierName='" + carrierName + '\'' +
                ", estimatedTransitHours=" + estimatedTransitHours +
                ", estimatedCost=" + estimatedCost +
                ", operationalRiskScore=" + operationalRiskScore +
                '}';
    }
}
