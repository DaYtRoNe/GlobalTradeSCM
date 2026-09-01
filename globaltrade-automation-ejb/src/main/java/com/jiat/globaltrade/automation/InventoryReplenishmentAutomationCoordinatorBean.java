package com.jiat.globaltrade.automation;

import com.jiat.globaltrade.service.AuditServiceBean;
import com.jiat.globaltrade.service.InventoryServiceBean;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Coordinator EJB for Automated Scheduled Inventory Replenishment.
 * Evaluates all warehouse items, ensures audit-backed and in-memory de-duplication
 * with a deterministic >= 60 minute cooldown window, and isolates per-item execution.
 * Caller identity propagates from entry-point bean. Zero direct persistence context coupling.
 */
@Stateless
@TransactionManagement(TransactionManagementType.CONTAINER)
public class InventoryReplenishmentAutomationCoordinatorBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(InventoryReplenishmentAutomationCoordinatorBean.class.getName());

    /** Deterministic cooldown window: 60 minutes to prevent redundant re-orders on successive 15-minute timer ticks */
    public static final long REPLENISHMENT_COOLDOWN_MINUTES = 60L;
    private static final long REPLENISHMENT_COOLDOWN_MS = REPLENISHMENT_COOLDOWN_MINUTES * 60 * 1000L;
    private static final Map<Long, Long> LAST_REPLENISHED_MAP = new ConcurrentHashMap<>();

    @EJB
    private InventoryServiceBean inventoryService;

    @EJB
    private AuditServiceBean auditService;

    @EJB
    private InventoryReplenishmentAutomationWorkerBean workerBean;

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public ReplenishmentBatchSummary evaluateAndReplenishAllItems() {
        LOGGER.log(Level.INFO, "[Automation-Coordinator] Starting scheduled inventory replenishment assessment (Cooldown: {0} mins)...",
                REPLENISHMENT_COOLDOWN_MINUTES);

        List<Long> itemIds = inventoryService.findAllInventoryItemIds();

        int total = itemIds != null ? itemIds.size() : 0;
        int requested = 0;
        int skipped = 0;
        int failed = 0;
        long now = System.currentTimeMillis();
        LocalDateTime auditCutoff = LocalDateTime.now().minusMinutes(REPLENISHMENT_COOLDOWN_MINUTES);

        if (itemIds != null) {
            for (Long itemId : itemIds) {
                // 1. Fast-path in-memory check
                Long lastReplenished = LAST_REPLENISHED_MAP.get(itemId);
                if (lastReplenished != null && (now - lastReplenished) < REPLENISHMENT_COOLDOWN_MS) {
                    LOGGER.log(Level.INFO, "[Automation-Coordinator] Skipping Item #{0} - in-memory replenishment cooldown active.", itemId);
                    skipped++;
                    continue;
                }

                // 2. Persistent audit-backed check via core AuditServiceBean (resilient across server restarts)
                try {
                    boolean hasRecentAudit = auditService.hasRecentAction(
                            "InventoryItem", itemId, "AUTO_REPLENISHMENT_REQUESTED", auditCutoff);

                    if (hasRecentAudit) {
                        LOGGER.log(Level.INFO, "[Automation-Coordinator] Skipping Item #{0} - persisted replenishment audit log exists within last {1} mins.",
                                new Object[]{itemId, REPLENISHMENT_COOLDOWN_MINUTES});
                        LAST_REPLENISHED_MAP.put(itemId, now);
                        skipped++;
                        continue;
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "[Automation-Coordinator] Could not verify audit dedup history for item #{0}: {1}",
                            new Object[]{itemId, e.getMessage()});
                }

                // 3. Delegate to worker in isolated REQUIRES_NEW transaction
                try {
                    InventoryReplenishmentAutomationWorkerBean.ReplenishmentEvaluationResult result =
                            workerBean.evaluateAndReplenish(itemId);

                    if (result.replenishmentRequested()) {
                        requested++;
                        LAST_REPLENISHED_MAP.put(itemId, now);
                    } else {
                        skipped++;
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "[Automation-Coordinator] Error evaluating replenishment for item #{0}: {1}",
                            new Object[]{itemId, e.getMessage()});
                    failed++;
                }
            }
        }

        LOGGER.log(Level.INFO, "[Automation-Coordinator] Replenishment assessment complete: total={0}, requested={1}, skipped={2}, failed={3}",
                new Object[]{total, requested, skipped, failed});

        return new ReplenishmentBatchSummary(total, requested, skipped, failed);
    }

    public static void clearDeduplicationCache() {
        LAST_REPLENISHED_MAP.clear();
    }

    public record ReplenishmentBatchSummary(int totalItems, int replenishmentOrdersPlaced, int skippedItems, int failedItems) implements Serializable {}
}
