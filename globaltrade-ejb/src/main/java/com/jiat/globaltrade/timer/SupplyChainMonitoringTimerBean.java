package com.jiat.globaltrade.timer;

import com.jiat.globaltrade.entity.CustomsDocument;
import com.jiat.globaltrade.entity.InventoryItem;
import com.jiat.globaltrade.entity.Shipment;
import com.jiat.globaltrade.entity.enums.CustomsDocumentStatus;
import com.jiat.globaltrade.entity.enums.ShipmentStatus;
import com.jiat.globaltrade.service.AuditServiceBean;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * =================================================================================================
 * DECLARATIVE EJB TIMER BEAN: SUPPLY CHAIN MONITORING
 * -------------------------------------------------------------------------------------------------
 * Design Decision & Architectural Justification:
 * 1. @Singleton + @Startup:
 *    Ensures a single container-managed lifecycle instance initializes on application startup,
 *    preventing redundant competing automatic timer registrations across EJB instance pools.
 *
 * 2. Persistent Timer (persistent = true):
 *    Payara's EJB timer service persists timer schedules in its container timer database.
 *    This guarantees that critical monitoring intervals survive application and server restarts.
 *
 * 3. Schedule Expression:
 *    @Schedule(hour = "*", minute = "* / 5", second = "0", persistent = true)
 *    Conceptually runs every 5 minutes in a production environment.
 *
 * 4. Duplicate Event Mitigation:
 *    Maintains an in-memory alert cache with a time-to-live cooldown (e.g. 30 minutes)
 *    to prevent flooding the audit log with duplicate alerts for the same condition
 *    during every single monitoring cycle.
 *
 * 5. Independent Audit Logging:
 *    Invokes AuditServiceBean (REQUIRES_NEW) to record monitoring findings without coupling
 *    to any long-running transactions.
 * =================================================================================================
 */
@Singleton
@Startup
@TransactionManagement(TransactionManagementType.CONTAINER)
public class SupplyChainMonitoringTimerBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(SupplyChainMonitoringTimerBean.class.getName());

    private static final long ALERT_COOLDOWN_MINUTES = 30;

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @EJB
    private AuditServiceBean auditService;

    // In-memory runtime observability statistics
    private volatile LocalDateTime lastMonitoringTime;
    private volatile long monitoringCycleCount = 0;
    private volatile int lastDetectedLowStockCount = 0;
    private volatile int lastDetectedDelayedShipmentCount = 0;
    private volatile int lastDetectedUrgentCustomsCount = 0;

    // Cache to prevent duplicate alert spamming across consecutive timer intervals
    // Key: "LOW_STOCK:" + itemId, "DELAYED_SHIPMENT:" + shipmentId, etc.
    // Value: Timestamp of last logged alert
    private final Map<String, LocalDateTime> alertCooldownCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        LOGGER.log(Level.INFO, "[SupplyChainMonitoringTimerBean] Initialized and active. Declarative @Schedule timer registered with Payara container.");
    }

    /**
     * Declarative / Automatic Timer Callback.
     * Triggered automatically by the Payara EJB Timer Service every 5 minutes on the hour.
     */
    @Schedule(hour = "*", minute = "*/5", second = "0", persistent = true, info = "DeclarativeSupplyChainMonitoringTimer")
    public void automaticMonitoringSchedule() {
        LOGGER.log(Level.INFO, "[SupplyChainMonitoringTimerBean] Automatic @Schedule timer triggered by Payara container.");
        runMonitoringCycle("AUTOMATIC_SCHEDULED_TIMER");
    }

    /**
     * Core monitoring business logic.
     * Can be invoked by the declarative @Schedule timer or manually via verification REST endpoint.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public MonitoringSummary runMonitoringCycle(String triggerSource) {
        LocalDateTime cycleStartTime = LocalDateTime.now();
        monitoringCycleCount++;
        lastMonitoringTime = cycleStartTime;

        LOGGER.log(Level.INFO, "[SupplyChainMonitoringTimerBean] Running monitoring cycle #{0} (Trigger: {1})...",
                new Object[]{monitoringCycleCount, triggerSource});

        // 1. Inventory Monitoring: Items at or below reorder level
        List<InventoryItem> lowStockItems = em.createQuery(
                "SELECT i FROM InventoryItem i WHERE i.quantity <= i.reorderLevel", InventoryItem.class)
                .getResultList();
        lastDetectedLowStockCount = lowStockItems.size();

        for (InventoryItem item : lowStockItems) {
            String cacheKey = "LOW_STOCK:" + item.getId();
            if (shouldEmitAlert(cacheKey)) {
                LOGGER.log(Level.WARNING, "[SupplyChainMonitoringTimerBean] Low stock detected: SKU {0}, Current: {1}, Reorder Level: {2}",
                        new Object[]{item.getSku(), item.getQuantity(), item.getReorderLevel()});

                auditService.logAction("LOW_STOCK_DETECTED", "InventoryItem", item.getId(), triggerSource,
                        String.format("Stock level (%d) at or below reorder threshold (%d) for SKU %s",
                                item.getQuantity(), item.getReorderLevel(), item.getSku()));

                alertCooldownCache.put(cacheKey, cycleStartTime);
            }
        }

        // 2. Shipment Monitoring: Non-delivered shipments past expected delivery date
        LocalDate today = LocalDate.now();
        List<Shipment> delayedShipments = em.createQuery(
                "SELECT s FROM Shipment s WHERE s.shipmentStatus <> :deliveredStatus AND s.expectedDeliveryDate < :today", Shipment.class)
                .setParameter("deliveredStatus", ShipmentStatus.DELIVERED)
                .setParameter("today", today)
                .getResultList();
        lastDetectedDelayedShipmentCount = delayedShipments.size();

        for (Shipment shipment : delayedShipments) {
            String cacheKey = "DELAYED_SHIPMENT:" + shipment.getId();
            if (shouldEmitAlert(cacheKey)) {
                LOGGER.log(Level.WARNING, "[SupplyChainMonitoringTimerBean] Delayed shipment detected: Tracking {0}, Expected: {1}, Status: {2}",
                        new Object[]{shipment.getTrackingNumber(), shipment.getExpectedDeliveryDate(), shipment.getShipmentStatus()});

                auditService.logAction("SHIPMENT_DELAY_DETECTED", "Shipment", shipment.getId(), triggerSource,
                        String.format("Shipment %s is delayed. Expected delivery was %s. Current status: %s",
                                shipment.getTrackingNumber(), shipment.getExpectedDeliveryDate(), shipment.getShipmentStatus()));

                alertCooldownCache.put(cacheKey, cycleStartTime);
            }
        }

        // 3. Customs Deadline Monitoring: Pending/Submitted documents approaching deadline (within 2 days or past)
        LocalDate deadlineThreshold = today.plusDays(2);
        List<CustomsDocumentStatus> activeStatuses = Arrays.asList(CustomsDocumentStatus.PENDING, CustomsDocumentStatus.SUBMITTED);
        List<CustomsDocument> urgentCustomsDocs = em.createQuery(
                "SELECT c FROM CustomsDocument c WHERE c.status IN :activeStatuses AND c.submissionDeadline <= :threshold", CustomsDocument.class)
                .setParameter("activeStatuses", activeStatuses)
                .setParameter("threshold", deadlineThreshold)
                .getResultList();
        lastDetectedUrgentCustomsCount = urgentCustomsDocs.size();

        for (CustomsDocument doc : urgentCustomsDocs) {
            String cacheKey = "CUSTOMS_DEADLINE:" + doc.getId();
            if (shouldEmitAlert(cacheKey)) {
                LOGGER.log(Level.WARNING, "[SupplyChainMonitoringTimerBean] Urgent customs deadline detected: Doc {0}, Deadline: {1}, Status: {2}",
                        new Object[]{doc.getDocumentNumber(), doc.getSubmissionDeadline(), doc.getStatus()});

                auditService.logAction("CUSTOMS_DEADLINE_APPROACHING", "CustomsDocument", doc.getId(), triggerSource,
                        String.format("Customs document %s requires attention. Deadline: %s, Current status: %s",
                                doc.getDocumentNumber(), doc.getSubmissionDeadline(), doc.getStatus()));

                alertCooldownCache.put(cacheKey, cycleStartTime);
            }
        }

        LOGGER.log(Level.INFO, "[SupplyChainMonitoringTimerBean] Monitoring cycle #{0} finished. Low Stock={1}, Delayed Shipments={2}, Urgent Customs={3}",
                new Object[]{monitoringCycleCount, lastDetectedLowStockCount, lastDetectedDelayedShipmentCount, lastDetectedUrgentCustomsCount});

        return new MonitoringSummary(
                monitoringCycleCount,
                triggerSource,
                cycleStartTime,
                lastDetectedLowStockCount,
                lastDetectedDelayedShipmentCount,
                lastDetectedUrgentCustomsCount
        );
    }

    private boolean shouldEmitAlert(String cacheKey) {
        LocalDateTime lastAlerted = alertCooldownCache.get(cacheKey);
        if (lastAlerted == null) {
            return true;
        }
        return lastAlerted.plusMinutes(ALERT_COOLDOWN_MINUTES).isBefore(LocalDateTime.now());
    }

    public void clearAlertCooldownCache() {
        alertCooldownCache.clear();
    }

    public LocalDateTime getLastMonitoringTime() {
        return lastMonitoringTime;
    }

    public long getMonitoringCycleCount() {
        return monitoringCycleCount;
    }

    public int getLastDetectedLowStockCount() {
        return lastDetectedLowStockCount;
    }

    public int getLastDetectedDelayedShipmentCount() {
        return lastDetectedDelayedShipmentCount;
    }

    public int getLastDetectedUrgentCustomsCount() {
        return lastDetectedUrgentCustomsCount;
    }

    /**
     * DTO summarizing the outcome of a monitoring cycle.
     */
    public static class MonitoringSummary implements Serializable {
        private static final long serialVersionUID = 1L;

        private final long cycleNumber;
        private final String triggerSource;
        private final LocalDateTime executionTime;
        private final int lowStockCount;
        private final int delayedShipmentsCount;
        private final int urgentCustomsDocsCount;

        public MonitoringSummary(long cycleNumber, String triggerSource, LocalDateTime executionTime,
                                 int lowStockCount, int delayedShipmentsCount, int urgentCustomsDocsCount) {
            this.cycleNumber = cycleNumber;
            this.triggerSource = triggerSource;
            this.executionTime = executionTime;
            this.lowStockCount = lowStockCount;
            this.delayedShipmentsCount = delayedShipmentsCount;
            this.urgentCustomsDocsCount = urgentCustomsDocsCount;
        }

        public long getCycleNumber() {
            return cycleNumber;
        }

        public String getTriggerSource() {
            return triggerSource;
        }

        public LocalDateTime getExecutionTime() {
            return executionTime;
        }

        public int getLowStockCount() {
            return lowStockCount;
        }

        public int getDelayedShipmentsCount() {
            return delayedShipmentsCount;
        }

        public int getUrgentCustomsDocsCount() {
            return urgentCustomsDocsCount;
        }
    }
}
