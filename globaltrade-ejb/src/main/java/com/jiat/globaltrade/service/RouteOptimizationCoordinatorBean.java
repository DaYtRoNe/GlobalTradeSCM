package com.jiat.globaltrade.service;

import com.jiat.globaltrade.entity.enums.ShipmentStatus;
import com.jiat.globaltrade.service.RouteOptimizationWorkerBean.SingleShipmentOptimizationResult;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Top-level Coordinator for Enterprise Route Optimization.
 *
 * Transaction Isolation Pattern:
 * - @TransactionAttribute(NOT_SUPPORTED): The coordinator avoids a single monolithic business transaction.
 * - Injects RouteOptimizationWorkerBean across EJB proxy boundary.
 * - Invokes worker methods which execute with @TransactionAttribute(REQUIRES_NEW) for each shipment.
 * - Catches individual shipment errors without aborting optimization for subsequent active shipments.
 */
@Stateless
@TransactionManagement(TransactionManagementType.CONTAINER)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class RouteOptimizationCoordinatorBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(RouteOptimizationCoordinatorBean.class.getName());

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @EJB
    private RouteOptimizationWorkerBean workerBean;

    /**
     * Executes batch route optimization across all active eligible shipments.
     */
    public RouteOptimizationBatchSummary optimizeAllActiveShipments(String triggerSource, String performedBy) {
        LocalDateTime startTime = LocalDateTime.now();
        LOGGER.log(Level.INFO, "[RouteCoordinator] Starting batch route optimization (Trigger: {0})...", triggerSource);

        // Query eligible active shipment IDs (PENDING, IN_TRANSIT, CUSTOMS_HOLD)
        List<Long> activeShipmentIds = em.createQuery(
                "SELECT s.id FROM Shipment s WHERE s.shipmentStatus <> :delivered AND s.shipmentStatus <> :cancelled ORDER BY s.id",
                Long.class)
                .setParameter("delivered", ShipmentStatus.DELIVERED)
                .setParameter("cancelled", ShipmentStatus.CANCELLED)
                .getResultList();

        List<SingleShipmentOptimizationResult> results = new ArrayList<>();
        int successfulCount = 0;
        int failedCount = 0;
        int skippedCount = 0;

        for (Long shipmentId : activeShipmentIds) {
            try {
                SingleShipmentOptimizationResult res = workerBean.optimizeSingleShipment(shipmentId, triggerSource, performedBy);
                results.add(res);

                if (res.isSuccess()) {
                    successfulCount++;
                } else if (res.isSkipped()) {
                    skippedCount++;
                } else {
                    failedCount++;
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "[RouteCoordinator] Unexpected error evaluating shipment #" + shipmentId + ": " + e.getMessage(), e);
                results.add(SingleShipmentOptimizationResult.failure(shipmentId, e.getMessage()));
                failedCount++;
            }
        }

        String overallStatus;
        if (failedCount == 0) {
            overallStatus = "SUCCESS";
        } else if (successfulCount > 0) {
            overallStatus = "PARTIAL_FAILURE";
        } else {
            overallStatus = "FAILURE";
        }

        LOGGER.log(Level.INFO, "[RouteCoordinator] Route optimization complete. Status: {0} (Total: {1}, Success: {2}, Failed: {3}, Skipped: {4})",
                new Object[]{overallStatus, activeShipmentIds.size(), successfulCount, failedCount, skippedCount});

        return new RouteOptimizationBatchSummary(
                overallStatus,
                triggerSource,
                startTime,
                activeShipmentIds.size(),
                successfulCount,
                failedCount,
                skippedCount,
                results
        );
    }

    /**
     * DTO describing the aggregated outcome of a route optimization batch run.
     */
    public static class RouteOptimizationBatchSummary implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String overallStatus;
        private final String triggerSource;
        private final LocalDateTime executionTime;
        private final int totalShipmentsEvaluated;
        private final int successfulOptimizations;
        private final int failedOptimizations;
        private final int skippedShipments;
        private final List<SingleShipmentOptimizationResult> results;

        public RouteOptimizationBatchSummary(
                String overallStatus, String triggerSource, LocalDateTime executionTime,
                int totalShipmentsEvaluated, int successfulOptimizations, int failedOptimizations,
                int skippedShipments, List<SingleShipmentOptimizationResult> results) {
            this.overallStatus = overallStatus;
            this.triggerSource = triggerSource;
            this.executionTime = executionTime;
            this.totalShipmentsEvaluated = totalShipmentsEvaluated;
            this.successfulOptimizations = successfulOptimizations;
            this.failedOptimizations = failedOptimizations;
            this.skippedShipments = skippedShipments;
            this.results = Collections.unmodifiableList(results);
        }

        public String getOverallStatus() {
            return overallStatus;
        }

        public String getTriggerSource() {
            return triggerSource;
        }

        public LocalDateTime getExecutionTime() {
            return executionTime;
        }

        public int getTotalShipmentsEvaluated() {
            return totalShipmentsEvaluated;
        }

        public int getSuccessfulOptimizations() {
            return successfulOptimizations;
        }

        public int getFailedOptimizations() {
            return failedOptimizations;
        }

        public int getSkippedShipments() {
            return skippedShipments;
        }

        public List<SingleShipmentOptimizationResult> getResults() {
            return results;
        }
    }
}
