package com.jiat.globaltrade.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity representing the single active route recommendation for an active shipment consignment.
 * Enforces one current recommendation per shipment via unique constraint on shipment_id.
 */
@Entity
@Table(name = "route_optimization_recommendations")
public class RouteOptimizationRecommendation implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false, unique = true)
    private Shipment shipment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "route_option_id", nullable = false)
    private RouteOption selectedRoute;

    @Column(name = "optimization_score", nullable = false, precision = 5, scale = 4)
    private BigDecimal optimizationScore;

    @Column(name = "transit_time_hours", nullable = false)
    private Integer transitTimeHours;

    @Column(name = "estimated_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal estimatedCost;

    @Column(name = "risk_score", nullable = false, precision = 3, scale = 2)
    private BigDecimal riskScore;

    @Column(name = "evaluated_at", nullable = false)
    private LocalDateTime evaluatedAt;

    @Column(name = "evaluation_source", nullable = false, length = 50)
    private String evaluationSource;

    @Column(name = "summary_rationale", length = 500)
    private String summaryRationale;

    public RouteOptimizationRecommendation() {
    }

    public RouteOptimizationRecommendation(Shipment shipment, RouteOption selectedRoute,
                                           BigDecimal optimizationScore, Integer transitTimeHours,
                                           BigDecimal estimatedCost, BigDecimal riskScore,
                                           String evaluationSource, String summaryRationale) {
        this.shipment = shipment;
        this.selectedRoute = selectedRoute;
        this.optimizationScore = optimizationScore;
        this.transitTimeHours = transitTimeHours;
        this.estimatedCost = estimatedCost;
        this.riskScore = riskScore;
        this.evaluationSource = evaluationSource;
        this.summaryRationale = summaryRationale;
        this.evaluatedAt = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.evaluatedAt == null) {
            this.evaluatedAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Shipment getShipment() {
        return shipment;
    }

    public void setShipment(Shipment shipment) {
        this.shipment = shipment;
    }

    public RouteOption getSelectedRoute() {
        return selectedRoute;
    }

    public void setSelectedRoute(RouteOption selectedRoute) {
        this.selectedRoute = selectedRoute;
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

    public LocalDateTime getEvaluatedAt() {
        return evaluatedAt;
    }

    public void setEvaluatedAt(LocalDateTime evaluatedAt) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RouteOptimizationRecommendation that = (RouteOptimizationRecommendation) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "RouteOptimizationRecommendation{" +
                "id=" + id +
                ", shipmentId=" + (shipment != null ? shipment.getId() : null) +
                ", routeCode=" + (selectedRoute != null ? selectedRoute.getRouteCode() : null) +
                ", score=" + optimizationScore +
                '}';
    }
}
