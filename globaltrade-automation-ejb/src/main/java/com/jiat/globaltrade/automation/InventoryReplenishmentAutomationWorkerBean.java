package com.jiat.globaltrade.automation;

import com.jiat.globaltrade.entity.InventoryItem;
import com.jiat.globaltrade.integration.gateway.SupplierPortalGateway;
import com.jiat.globaltrade.integration.gateway.WarehouseManagementGateway;
import com.jiat.globaltrade.service.AuditServiceBean;
import com.jiat.globaltrade.service.InventoryServiceBean;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Worker EJB for Automated Scheduled Inventory Replenishment Workflow.
 * Identifies low-stock items and dispatches replenishment requests to WMS and Supplier gateways in REQUIRES_NEW.
 * Caller identity (SYSTEM from timer, ADMIN from diagnostic/test) propagates from the entry-point bean.
 * Uses core InventoryServiceBean and AuditServiceBean via EJB injection.
 */
@Stateless
@TransactionManagement(TransactionManagementType.CONTAINER)
public class InventoryReplenishmentAutomationWorkerBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(InventoryReplenishmentAutomationWorkerBean.class.getName());

    @EJB
    private InventoryServiceBean inventoryService;

    @EJB
    private WarehouseManagementGateway warehouseGateway;

    @EJB
    private SupplierPortalGateway supplierGateway;

    @EJB
    private AuditServiceBean auditService;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ReplenishmentEvaluationResult evaluateAndReplenish(Long itemId) {
        if (itemId == null) {
            return new ReplenishmentEvaluationResult(null, null, false, 0, "Invalid item ID");
        }

        InventoryItem item = inventoryService.findInventoryItemById(itemId);
        if (item == null) {
            return new ReplenishmentEvaluationResult(itemId, null, false, 0, "Item not found");
        }

        int currentQty = item.getQuantity();
        int reorderLevel = item.getReorderLevel();

        if (currentQty <= reorderLevel) {
            int replenishmentQty = Math.max((reorderLevel * 2) - currentQty, 100);

            LOGGER.log(Level.INFO, "[Automation-Worker] Low stock detected for SKU {0} (Qty: {1}, ReorderLevel: {2}). Dispatching replenishment of {3} units.",
                    new Object[]{item.getSku(), currentQty, reorderLevel, replenishmentQty});

            // 1. Dispatch internal WMS replenishment transfer
            warehouseGateway.requestReplenishmentOrder(item.getSku(), replenishmentQty, "WH-SIN-01");

            // 2. Transmit B2B Purchase Order to Supplier Portal
            supplierGateway.transmitPurchaseOrder("VND-001", item.getSku(), replenishmentQty, "FOB_PORT_TERMINAL");

            // 3. Log enterprise audit event
            auditService.logAction(
                    "AUTO_REPLENISHMENT_REQUESTED",
                    "InventoryItem",
                    item.getId(),
                    "SYSTEM_AUTOMATION",
                    String.format("Triggered automated stock replenishment for SKU %s. Current: %d, Reorder Level: %d, Requested: %d units (Mode: SIMULATED)",
                            item.getSku(), currentQty, reorderLevel, replenishmentQty)
            );

            return new ReplenishmentEvaluationResult(item.getId(), item.getSku(), true, replenishmentQty,
                    "Automated replenishment order dispatched to WMS and Supplier Portal");
        }

        return new ReplenishmentEvaluationResult(item.getId(), item.getSku(), false, 0,
                "Stock nominal. No replenishment required.");
    }

    public record ReplenishmentEvaluationResult(
            Long itemId,
            String sku,
            boolean replenishmentRequested,
            int requestedQuantity,
            String message
    ) implements Serializable {}
}
