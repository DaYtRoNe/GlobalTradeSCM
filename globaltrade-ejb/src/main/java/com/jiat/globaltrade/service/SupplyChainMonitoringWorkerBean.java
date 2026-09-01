package com.jiat.globaltrade.service;

import com.jiat.globaltrade.entity.CustomsDocument;
import com.jiat.globaltrade.entity.InventoryItem;
import com.jiat.globaltrade.entity.Shipment;
import com.jiat.globaltrade.entity.SupplyChainAlert;
import com.jiat.globaltrade.entity.Vendor;
import com.jiat.globaltrade.entity.enums.CustomsDocumentStatus;
import com.jiat.globaltrade.entity.enums.ShipmentStatus;
import com.jiat.globaltrade.entity.enums.SupplyChainAlertStatus;
import com.jiat.globaltrade.entity.enums.SupplyChainAlertType;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Worker EJB for isolated category monitoring evaluations.
 * Each category method executes in an independent REQUIRES_NEW transaction,
 * guaranteeing that a failure in one category does not rollback other categories.
 */
@Stateless
@TransactionManagement(TransactionManagementType.CONTAINER)
public class SupplyChainMonitoringWorkerBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(SupplyChainMonitoringWorkerBean.class.getName());

    /** Clearly documented project business constant for vendor risk threshold. */
    public static final BigDecimal MIN_ACCEPTABLE_VENDOR_RATING = new BigDecimal("3.00");

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @EJB
    private SupplyChainAlertServiceBean alertService;

    /**
     * Evaluates Rule 1: Shipment Delays in an independent transaction.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public CategoryResult evaluateShipmentAlerts(String triggerSource) {
        LOGGER.log(Level.INFO, "[Worker] Evaluating Shipment Delays (Trigger: {0})...", triggerSource);
        LocalDate today = LocalDate.now();
        int evaluated = 0;
        int activeAlerts = 0;
        int resolvedAlerts = 0;

        // 1. Evaluate all active (non-delivered, non-cancelled) shipments
        List<Shipment> activeShipments = em.createQuery(
                "SELECT s FROM Shipment s WHERE s.shipmentStatus <> :delivered AND s.shipmentStatus <> :cancelled", Shipment.class)
                .setParameter("delivered", ShipmentStatus.DELIVERED)
                .setParameter("cancelled", ShipmentStatus.CANCELLED)
                .getResultList();

        for (Shipment s : activeShipments) {
            evaluated++;
            String alertKey = "SHIPMENT_DELAY:" + s.getId();
            if (s.getExpectedDeliveryDate() != null && s.getExpectedDeliveryDate().isBefore(today)) {
                String msg = String.format("Shipment %s is past its expected delivery date (%s). Current status: %s",
                        s.getTrackingNumber(), s.getExpectedDeliveryDate(), s.getShipmentStatus());
                alertService.processActiveCondition(alertKey, SupplyChainAlertType.SHIPMENT_DELAY, "Shipment", s.getId(), msg, triggerSource);
                activeAlerts++;
            } else {
                if (alertService.processClearedCondition(alertKey, triggerSource)) {
                    resolvedAlerts++;
                }
            }
        }

        // 2. Resolve delay alerts for shipments that have since transitioned to DELIVERED or CANCELLED
        List<SupplyChainAlert> existingDelayAlerts = em.createQuery(
                "SELECT a FROM SupplyChainAlert a WHERE a.alertType = :type AND a.alertStatus <> :resolved", SupplyChainAlert.class)
                .setParameter("type", SupplyChainAlertType.SHIPMENT_DELAY)
                .setParameter("resolved", SupplyChainAlertStatus.RESOLVED)
                .getResultList();

        for (SupplyChainAlert alert : existingDelayAlerts) {
            Shipment s = em.find(Shipment.class, alert.getEntityId());
            if (s == null || s.getShipmentStatus() == ShipmentStatus.DELIVERED || s.getShipmentStatus() == ShipmentStatus.CANCELLED) {
                if (alertService.processClearedCondition(alert.getAlertKey(), triggerSource)) {
                    resolvedAlerts++;
                }
            }
        }

        return new CategoryResult("SHIPMENT_DELAY", true, null, evaluated, activeAlerts, resolvedAlerts);
    }

    /**
     * Evaluates Rule 2: Inventory Shortages / Replenishment Required in an independent transaction.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public CategoryResult evaluateInventoryAlerts(String triggerSource) {
        LOGGER.log(Level.INFO, "[Worker] Evaluating Inventory Stock Levels (Trigger: {0})...", triggerSource);
        int evaluated = 0;
        int activeAlerts = 0;
        int resolvedAlerts = 0;

        List<InventoryItem> items = em.createQuery("SELECT i FROM InventoryItem i", InventoryItem.class).getResultList();

        for (InventoryItem i : items) {
            evaluated++;
            String alertKey = "INVENTORY_REPLENISHMENT_REQUIRED:" + i.getId();
            if (i.getQuantity() <= i.getReorderLevel()) {
                String msg = String.format("Stock level (%d units) for SKU %s (%s) is at or below reorder threshold (%d units).",
                        i.getQuantity(), i.getSku(), i.getItemName(), i.getReorderLevel());
                alertService.processActiveCondition(alertKey, SupplyChainAlertType.INVENTORY_REPLENISHMENT_REQUIRED, "InventoryItem", i.getId(), msg, triggerSource);
                activeAlerts++;
            } else {
                if (alertService.processClearedCondition(alertKey, triggerSource)) {
                    resolvedAlerts++;
                }
            }
        }

        return new CategoryResult("INVENTORY_REPLENISHMENT_REQUIRED", true, null, evaluated, activeAlerts, resolvedAlerts);
    }

    /**
     * Evaluates Rule 3: Vendor Performance Risk in an independent transaction.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public CategoryResult evaluateVendorAlerts(String triggerSource) {
        LOGGER.log(Level.INFO, "[Worker] Evaluating Vendor Performance Ratings (Trigger: {0})...", triggerSource);
        int evaluated = 0;
        int activeAlerts = 0;
        int resolvedAlerts = 0;

        List<Vendor> vendors = em.createQuery("SELECT v FROM Vendor v", Vendor.class).getResultList();

        for (Vendor v : vendors) {
            evaluated++;
            String alertKey = "VENDOR_PERFORMANCE_RISK:" + v.getId();
            if (v.getPerformanceRating() != null && v.getPerformanceRating().compareTo(MIN_ACCEPTABLE_VENDOR_RATING) < 0) {
                String msg = String.format("Vendor %s (%s) performance rating (%s) is below acceptable threshold (%s).",
                        v.getCompanyName(), v.getVendorCode(), v.getPerformanceRating(), MIN_ACCEPTABLE_VENDOR_RATING);
                alertService.processActiveCondition(alertKey, SupplyChainAlertType.VENDOR_PERFORMANCE_RISK, "Vendor", v.getId(), msg, triggerSource);
                activeAlerts++;
            } else if (v.getPerformanceRating() != null && v.getPerformanceRating().compareTo(MIN_ACCEPTABLE_VENDOR_RATING) >= 0) {
                if (alertService.processClearedCondition(alertKey, triggerSource)) {
                    resolvedAlerts++;
                }
            }
        }

        return new CategoryResult("VENDOR_PERFORMANCE_RISK", true, null, evaluated, activeAlerts, resolvedAlerts);
    }

    /**
     * Evaluates Rule 4: Customs Document Filing Deadlines in an independent transaction.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public CategoryResult evaluateCustomsAlerts(String triggerSource) {
        LOGGER.log(Level.INFO, "[Worker] Evaluating Customs Filing Deadlines (Trigger: {0})...", triggerSource);
        LocalDate today = LocalDate.now();
        int evaluated = 0;
        int activeAlerts = 0;
        int resolvedAlerts = 0;

        List<CustomsDocument> docs = em.createQuery("SELECT c FROM CustomsDocument c", CustomsDocument.class).getResultList();

        for (CustomsDocument c : docs) {
            evaluated++;
            String alertKey = "CUSTOMS_DOCUMENT_DEADLINE:" + c.getId();
            boolean isPending = (c.getStatus() != CustomsDocumentStatus.APPROVED && c.getStatus() != CustomsDocumentStatus.REJECTED);
            boolean isOverdueOrDueToday = (c.getSubmissionDeadline() != null && !c.getSubmissionDeadline().isAfter(today));

            if (isPending && isOverdueOrDueToday) {
                String msg = String.format("Customs document %s (%s) deadline (%s) is due or overdue. Status: %s",
                        c.getDocumentNumber(), c.getDocumentType(), c.getSubmissionDeadline(), c.getStatus());
                alertService.processActiveCondition(alertKey, SupplyChainAlertType.CUSTOMS_DOCUMENT_DEADLINE, "CustomsDocument", c.getId(), msg, triggerSource);
                activeAlerts++;
            } else {
                if (alertService.processClearedCondition(alertKey, triggerSource)) {
                    resolvedAlerts++;
                }
            }
        }

        return new CategoryResult("CUSTOMS_DOCUMENT_DEADLINE", true, null, evaluated, activeAlerts, resolvedAlerts);
    }

    /**
     * Immutable value object holding the outcome of a single category monitoring evaluation.
     */
    public static class CategoryResult implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String categoryName;
        private final boolean success;
        private final String errorMessage;
        private final int entitiesEvaluated;
        private final int activeAlertsDetected;
        private final int alertsResolved;

        public CategoryResult(String categoryName, boolean success, String errorMessage,
                              int entitiesEvaluated, int activeAlertsDetected, int alertsResolved) {
            this.categoryName = categoryName;
            this.success = success;
            this.errorMessage = errorMessage;
            this.entitiesEvaluated = entitiesEvaluated;
            this.activeAlertsDetected = activeAlertsDetected;
            this.alertsResolved = alertsResolved;
        }

        public static CategoryResult failure(String categoryName, String errorMessage) {
            return new CategoryResult(categoryName, false, errorMessage, 0, 0, 0);
        }

        public String getCategoryName() {
            return categoryName;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public int getEntitiesEvaluated() {
            return entitiesEvaluated;
        }

        public int getActiveAlertsDetected() {
            return activeAlertsDetected;
        }

        public int getAlertsResolved() {
            return alertsResolved;
        }
    }
}
