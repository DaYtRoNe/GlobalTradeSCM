package com.jiat.globaltrade.service;

import com.jiat.globaltrade.service.SupplyChainMonitoringWorkerBean.CategoryResult;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Top-Level Supply Chain Monitoring Coordinator Service Bean.
 *
 * Transaction & Failure Isolation Architecture:
 * - Annotated with @TransactionAttribute(NOT_SUPPORTED): The coordinator does not manage
 *   a monolithic business transaction across all rules.
 * - Injects SupplyChainMonitoringWorkerBean via @EJB: Crosses an EJB container proxy boundary.
 * - Invokes worker methods which execute with @TransactionAttribute(REQUIRES_NEW).
 * - If a persistence failure occurs in one category (e.g. inventory), that category rolls back,
 *   while subsequent categories (e.g. vendors, customs) still execute and commit successfully.
 * - Produces an overall evaluation status: SUCCESS, PARTIAL_FAILURE, or FAILURE.
 */
@Stateless
@TransactionManagement(TransactionManagementType.CONTAINER)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class SupplyChainMonitoringServiceBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(SupplyChainMonitoringServiceBean.class.getName());

    @EJB
    private SupplyChainMonitoringWorkerBean workerBean;

    /**
     * Executes a complete multi-category supply chain monitoring cycle.
     * Coordinates the 4 monitoring rule groups with transaction-level failure isolation.
     */
    public SupplyChainEvaluationResult evaluateSupplyChain(String triggerSource) {
        LocalDateTime startTime = LocalDateTime.now();
        LOGGER.log(Level.INFO, "[MonitoringCoordinator] Starting automated monitoring cycle (Trigger: {0})...", triggerSource);

        List<CategoryResult> categoryResults = new ArrayList<>();

        // 1. Shipment Delays Category
        try {
            CategoryResult res = workerBean.evaluateShipmentAlerts(triggerSource);
            categoryResults.add(res);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[MonitoringCoordinator] Shipment monitoring category failed: " + e.getMessage(), e);
            categoryResults.add(CategoryResult.failure("SHIPMENT_DELAY", e.getMessage()));
        }

        // 2. Inventory Replenishment Category
        try {
            CategoryResult res = workerBean.evaluateInventoryAlerts(triggerSource);
            categoryResults.add(res);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[MonitoringCoordinator] Inventory monitoring category failed: " + e.getMessage(), e);
            categoryResults.add(CategoryResult.failure("INVENTORY_REPLENISHMENT_REQUIRED", e.getMessage()));
        }

        // 3. Vendor Performance Category
        try {
            CategoryResult res = workerBean.evaluateVendorAlerts(triggerSource);
            categoryResults.add(res);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[MonitoringCoordinator] Vendor monitoring category failed: " + e.getMessage(), e);
            categoryResults.add(CategoryResult.failure("VENDOR_PERFORMANCE_RISK", e.getMessage()));
        }

        // 4. Customs Filing Deadlines Category
        try {
            CategoryResult res = workerBean.evaluateCustomsAlerts(triggerSource);
            categoryResults.add(res);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[MonitoringCoordinator] Customs monitoring category failed: " + e.getMessage(), e);
            categoryResults.add(CategoryResult.failure("CUSTOMS_DOCUMENT_DEADLINE", e.getMessage()));
        }

        // Aggregate results
        int successfulCategories = 0;
        int failedCategories = 0;
        int totalEvaluated = 0;
        int totalActiveAlerts = 0;
        int totalResolvedAlerts = 0;

        for (CategoryResult cr : categoryResults) {
            if (cr.isSuccess()) {
                successfulCategories++;
                totalEvaluated += cr.getEntitiesEvaluated();
                totalActiveAlerts += cr.getActiveAlertsDetected();
                totalResolvedAlerts += cr.getAlertsResolved();
            } else {
                failedCategories++;
            }
        }

        String overallStatus;
        if (failedCategories == 0) {
            overallStatus = "SUCCESS";
        } else if (successfulCategories > 0) {
            overallStatus = "PARTIAL_FAILURE";
        } else {
            overallStatus = "FAILURE";
        }

        LOGGER.log(Level.INFO, "[MonitoringCoordinator] Monitoring cycle finished. Status: {0} (Success: {1}/4, Failed: {2}/4, Active Alerts: {3}, Resolved: {4})",
                new Object[]{overallStatus, successfulCategories, failedCategories, totalActiveAlerts, totalResolvedAlerts});

        return new SupplyChainEvaluationResult(
                overallStatus,
                triggerSource,
                startTime,
                successfulCategories,
                failedCategories,
                totalEvaluated,
                totalActiveAlerts,
                totalResolvedAlerts,
                categoryResults
        );
    }

    /**
     * DTO summarizing the outcome of an entire multi-category monitoring cycle.
     */
    public static class SupplyChainEvaluationResult implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String overallStatus;
        private final String triggerSource;
        private final LocalDateTime executionTime;
        private final int successfulCategories;
        private final int failedCategories;
        private final int totalEntitiesEvaluated;
        private final int totalActiveAlertsDetected;
        private final int totalAlertsResolved;
        private final List<CategoryResult> categoryResults;

        public SupplyChainEvaluationResult(
                String overallStatus,
                String triggerSource,
                LocalDateTime executionTime,
                int successfulCategories,
                int failedCategories,
                int totalEntitiesEvaluated,
                int totalActiveAlertsDetected,
                int totalAlertsResolved,
                List<CategoryResult> categoryResults) {
            this.overallStatus = overallStatus;
            this.triggerSource = triggerSource;
            this.executionTime = executionTime;
            this.successfulCategories = successfulCategories;
            this.failedCategories = failedCategories;
            this.totalEntitiesEvaluated = totalEntitiesEvaluated;
            this.totalActiveAlertsDetected = totalActiveAlertsDetected;
            this.totalAlertsResolved = totalAlertsResolved;
            this.categoryResults = Collections.unmodifiableList(categoryResults);
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

        public int getSuccessfulCategories() {
            return successfulCategories;
        }

        public int getFailedCategories() {
            return failedCategories;
        }

        public int getTotalEntitiesEvaluated() {
            return totalEntitiesEvaluated;
        }

        public int getTotalActiveAlertsDetected() {
            return totalActiveAlertsDetected;
        }

        public int getTotalAlertsResolved() {
            return totalAlertsResolved;
        }

        public List<CategoryResult> getCategoryResults() {
            return categoryResults;
        }
    }
}
