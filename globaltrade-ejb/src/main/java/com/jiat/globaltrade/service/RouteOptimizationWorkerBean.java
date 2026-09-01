package com.jiat.globaltrade.service;

import com.jiat.globaltrade.entity.RouteOptimizationRecommendation;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Worker EJB performing route optimization evaluation for an individual shipment.
 * Executes in an independent REQUIRES_NEW transaction across EJB container proxy boundaries,
 * guaranteeing that an error on one shipment does not roll back optimizations for other shipments.
 */
@Stateless
@TransactionManagement(TransactionManagementType.CONTAINER)
public class RouteOptimizationWorkerBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(RouteOptimizationWorkerBean.class.getName());

    @EJB
    private RouteOptimizationServiceBean routeOptimizationService;

    /**
     * Optimizes route for a single shipment within an isolated transaction.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public SingleShipmentOptimizationResult optimizeSingleShipment(Long shipmentId, String triggerSource, String performedBy) {
        LOGGER.log(Level.FINE, "[Worker] Optimizing route for shipment #{0}...", shipmentId);

        try {
            RouteOptimizationRecommendation rec =
                    routeOptimizationService.optimizeShipmentRoute(shipmentId, triggerSource, performedBy);

            if (rec == null) {
                // Skipped (e.g. non-active shipment status)
                return SingleShipmentOptimizationResult.skipped(shipmentId, "Shipment is not in an active operational state");
            }

            return SingleShipmentOptimizationResult.success(
                    shipmentId,
                    rec.getSelectedRoute().getRouteCode(),
                    rec.getOptimizationScore(),
                    rec.getTransitTimeHours(),
                    rec.getEstimatedCost()
            );
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[Worker] Optimization failed for shipment #" + shipmentId + ": " + e.getMessage(), e);
            return SingleShipmentOptimizationResult.failure(shipmentId, e.getMessage());
        }
    }

    /**
     * Value object describing the outcome of a single shipment route optimization.
     */
    public static class SingleShipmentOptimizationResult implements Serializable {
        private static final long serialVersionUID = 1L;

        private final Long shipmentId;
        private final boolean success;
        private final boolean skipped;
        private final String routeCode;
        private final BigDecimal score;
        private final Integer transitHours;
        private final BigDecimal estimatedCost;
        private final String errorMessage;

        private SingleShipmentOptimizationResult(
                Long shipmentId, boolean success, boolean skipped, String routeCode,
                BigDecimal score, Integer transitHours, BigDecimal estimatedCost, String errorMessage) {
            this.shipmentId = shipmentId;
            this.success = success;
            this.skipped = skipped;
            this.routeCode = routeCode;
            this.score = score;
            this.transitHours = transitHours;
            this.estimatedCost = estimatedCost;
            this.errorMessage = errorMessage;
        }

        public static SingleShipmentOptimizationResult success(Long shipmentId, String routeCode, BigDecimal score, Integer hours, BigDecimal cost) {
            return new SingleShipmentOptimizationResult(shipmentId, true, false, routeCode, score, hours, cost, null);
        }

        public static SingleShipmentOptimizationResult skipped(Long shipmentId, String reason) {
            return new SingleShipmentOptimizationResult(shipmentId, false, true, null, null, null, null, reason);
        }

        public static SingleShipmentOptimizationResult failure(Long shipmentId, String error) {
            return new SingleShipmentOptimizationResult(shipmentId, false, false, null, null, null, null, error);
        }

        public Long getShipmentId() {
            return shipmentId;
        }

        public boolean isSuccess() {
            return success;
        }

        public boolean isSkipped() {
            return skipped;
        }

        public String getRouteCode() {
            return routeCode;
        }

        public BigDecimal getScore() {
            return score;
        }

        public Integer getTransitHours() {
            return transitHours;
        }

        public BigDecimal getEstimatedCost() {
            return estimatedCost;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
