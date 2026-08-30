package com.jiat.globaltrade.service;

import com.jiat.globaltrade.entity.InventoryItem;
import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.UserTransaction;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * =================================================================================================
 * ARCHITECTURAL NOTE ON TRANSACTION MANAGEMENT:
 * -------------------------------------------------------------------------------------------------
 * Container-Managed Transactions (CMT) is the primary and preferred enterprise standard across
 * the GlobalTrade SCM system because declarative boundaries (@TransactionAttribute) reduce
 * boilerplate, prevent resource leaks, and eliminate manual commit/rollback error scenarios.
 *
 * This bean demonstrates a narrowly scoped, highly justified Bean-Managed Transaction (BMT) use case:
 * An administrative Physical Inventory Reconciliation process where the business logic requires
 * fine-grained programmatic control over transaction demarcation (e.g. conditional rollbacks based
 * on complex discrepancy threshold evaluations and multi-phase validation without throwing runtime
 * exceptions).
 * =================================================================================================
 */
@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class InventoryReconciliationBean {

    private static final Logger LOGGER = Logger.getLogger(InventoryReconciliationBean.class.getName());

    @Resource
    private UserTransaction userTransaction;

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager entityManager;

    @EJB
    private AuditServiceBean auditService;

    /**
     * Programmatically managed transaction for inventory reconciliation:
     * 1. Begins transaction via UserTransaction.begin().
     * 2. Evaluates physical count against recorded system count.
     * 3. If count is invalid or exceeds allowable variance threshold, programmatically rolls back via UserTransaction.rollback().
     * 4. If acceptable, applies adjustment and commits via UserTransaction.commit().
     * 5. Employs independent audit logging (REQUIRES_NEW) to record reconciliation outcomes.
     */
    public ReconciliationResult reconcilePhysicalCount(Long inventoryItemId, int physicalCount, int maxDiscrepancyThreshold, String performedBy) {
        LOGGER.log(Level.INFO, "[InventoryReconciliationBean] [BMT] Starting programmatic reconciliation for item #{0}, physical count={1}",
                new Object[]{inventoryItemId, physicalCount});

        if (inventoryItemId == null || physicalCount < 0) {
            return new ReconciliationResult(false, "Invalid input: Item ID is null or physical count is negative.", 0, 0);
        }

        try {
            // Programmatically begin transaction boundary
            userTransaction.begin();
            entityManager.joinTransaction();

            InventoryItem item = entityManager.find(InventoryItem.class, inventoryItemId);
            if (item == null) {
                userTransaction.rollback();
                LOGGER.log(Level.WARNING, "[InventoryReconciliationBean] [BMT] Item not found. Transaction rolled back.");
                return new ReconciliationResult(false, "Inventory item not found for ID: " + inventoryItemId, 0, 0);
            }

            int systemCount = item.getQuantity();
            int discrepancy = physicalCount - systemCount;
            int absoluteDiscrepancy = Math.abs(discrepancy);

            // Programmatic business rule: Discrepancies exceeding the allowed variance threshold require formal manager approval
            if (absoluteDiscrepancy > maxDiscrepancyThreshold) {
                LOGGER.log(Level.WARNING, "[InventoryReconciliationBean] [BMT] Discrepancy ({0}) exceeds allowable threshold ({1}). Triggering programmatic rollback.",
                        new Object[]{absoluteDiscrepancy, maxDiscrepancyThreshold});

                userTransaction.rollback();

                // Audit log written via independent transaction (REQUIRES_NEW)
                auditService.logAction("RECONCILIATION_REJECTED", "InventoryItem", inventoryItemId, performedBy,
                        String.format("Discrepancy of %d exceeds threshold of %d. Programmatically rolled back.",
                                discrepancy, maxDiscrepancyThreshold));

                return new ReconciliationResult(false,
                        String.format("Reconciliation rejected: Discrepancy (%d) exceeds max threshold (%d).", discrepancy, maxDiscrepancyThreshold),
                        systemCount, physicalCount);
            }

            // Apply the reconciled quantity
            item.setQuantity(physicalCount);
            item.setLastUpdated(LocalDateTime.now());
            entityManager.merge(item);

            // Programmatically commit transaction boundary
            userTransaction.commit();
            LOGGER.log(Level.INFO, "[InventoryReconciliationBean] [BMT] Reconciliation committed successfully. Previous: {0}, Reconciled: {1}",
                    new Object[]{systemCount, physicalCount});

            auditService.logAction("RECONCILIATION_COMMITTED", "InventoryItem", inventoryItemId, performedBy,
                    String.format("Physical count updated from %d to %d (variance: %+d).", systemCount, physicalCount, discrepancy));

            return new ReconciliationResult(true, "Physical count successfully reconciled and committed.", systemCount, physicalCount);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[InventoryReconciliationBean] [BMT] Exception during programmatic transaction: " + e.getMessage(), e);
            try {
                if (userTransaction.getStatus() == jakarta.transaction.Status.STATUS_ACTIVE) {
                    userTransaction.rollback();
                }
            } catch (Exception rollbackEx) {
                LOGGER.log(Level.SEVERE, "[InventoryReconciliationBean] [BMT] Failed to rollback transaction: " + rollbackEx.getMessage(), rollbackEx);
            }
            return new ReconciliationResult(false, "Reconciliation failed due to internal error: " + e.getMessage(), 0, 0);
        }
    }

    /**
     * DTO containing reconciliation outcome details.
     */
    public static class ReconciliationResult {
        private final boolean success;
        private final String message;
        private final int previousCount;
        private final int reconciledCount;

        public ReconciliationResult(boolean success, String message, int previousCount, int reconciledCount) {
            this.success = success;
            this.message = message;
            this.previousCount = previousCount;
            this.reconciledCount = reconciledCount;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public int getPreviousCount() {
            return previousCount;
        }

        public int getReconciledCount() {
            return reconciledCount;
        }
    }
}
