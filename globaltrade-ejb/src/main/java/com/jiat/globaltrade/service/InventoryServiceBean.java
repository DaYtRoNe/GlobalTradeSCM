package com.jiat.globaltrade.service;

import com.jiat.globaltrade.entity.InventoryItem;
import com.jiat.globaltrade.exception.InsufficientInventoryException;
import com.jiat.globaltrade.security.SecurityRoles;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Core business service managing warehouse inventory levels, replenishment,
 * and stock validation using Container-Managed Transactions (CMT).
 */
@Stateless
@TransactionManagement(TransactionManagementType.CONTAINER)
@DeclareRoles({
        SecurityRoles.ADMIN,
        SecurityRoles.LOGISTICS_COORDINATOR,
        SecurityRoles.CUSTOMS_AGENT,
        SecurityRoles.WAREHOUSE_MANAGER,
        SecurityRoles.VENDOR_REPRESENTATIVE,
        SecurityRoles.CUSTOMER,
        SecurityRoles.SYSTEM
})
public class InventoryServiceBean {

    private static final Logger LOGGER = Logger.getLogger(InventoryServiceBean.class.getName());

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @EJB
    private AuditServiceBean auditService;

    /**
     * Read-only lookup of an inventory item by primary key.
     * SUPPORTS avoids creating an unnecessary transaction when invoked outside one.
     */
    @PermitAll
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public InventoryItem findInventoryItemById(Long id) {
        if (id == null) {
            return null;
        }
        return em.find(InventoryItem.class, id);
    }

    /**
     * Read-only lookup of all inventory items.
     */
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.WAREHOUSE_MANAGER, SecurityRoles.LOGISTICS_COORDINATOR})
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<InventoryItem> findAllInventoryItems() {
        return em.createQuery("SELECT i FROM InventoryItem i ORDER BY i.sku ASC", InventoryItem.class)
                .getResultList();
    }

    /**
     * Read-only lookup of all inventory item IDs.
     */
    @PermitAll
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Long> findAllInventoryItemIds() {
        return em.createQuery("SELECT i.id FROM InventoryItem i ORDER BY i.id ASC", Long.class)
                .getResultList();
    }

    /**
     * Increases inventory quantity (e.g. stock replenishment or return).
     * REQUIRED joins or creates a transaction.
     */
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.WAREHOUSE_MANAGER})
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public InventoryItem increaseStock(Long itemId, int quantity, String performedBy) {
        if (itemId == null || quantity <= 0) {
            throw new IllegalArgumentException("Item ID must not be null and quantity must be greater than zero.");
        }

        InventoryItem item = em.find(InventoryItem.class, itemId);
        if (item == null) {
            LOGGER.log(Level.WARNING, "[InventoryServiceBean] InventoryItem not found for ID: {0}", itemId);
            return null;
        }

        int previousQty = item.getQuantity();
        int newQty = previousQty + quantity;
        item.setQuantity(newQty);
        item.setLastUpdated(LocalDateTime.now());
        em.merge(item);

        LOGGER.log(Level.INFO, "[InventoryServiceBean] [REQUIRED] Stock increased for SKU {0} (ID: {1}) by {2}. New total: {3}",
                new Object[]{item.getSku(), itemId, quantity, newQty});

        auditService.logAction("STOCK_INCREASE", "InventoryItem", itemId, performedBy,
                String.format("Added %d units to SKU %s. Previous: %d, New: %d", quantity, item.getSku(), previousQty, newQty));

        return item;
    }

    /**
     * Decreases inventory quantity with business validation to prevent negative stock.
     * If available stock is insufficient, throws InsufficientInventoryException
     * which automatically triggers transaction rollback via @ApplicationException(rollback = true).
     */
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.WAREHOUSE_MANAGER})
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public InventoryItem decreaseStock(Long itemId, int quantity, String performedBy) throws InsufficientInventoryException {
        if (itemId == null || quantity <= 0) {
            throw new IllegalArgumentException("Item ID must not be null and quantity must be greater than zero.");
        }

        InventoryItem item = em.find(InventoryItem.class, itemId);
        if (item == null) {
            LOGGER.log(Level.WARNING, "[InventoryServiceBean] InventoryItem not found for ID: {0}", itemId);
            return null;
        }

        int currentQty = item.getQuantity();
        if (currentQty < quantity) {
            LOGGER.log(Level.WARNING, "[InventoryServiceBean] [REQUIRED] Insufficient inventory for item {0}. Available: {1}, Requested: {2}",
                    new Object[]{itemId, currentQty, quantity});
            throw new InsufficientInventoryException(itemId, quantity, currentQty);
        }

        int newQty = currentQty - quantity;
        item.setQuantity(newQty);
        item.setLastUpdated(LocalDateTime.now());
        em.merge(item);

        LOGGER.log(Level.INFO, "[InventoryServiceBean] [REQUIRED] Stock decreased for SKU {0} (ID: {1}) by {2}. Remaining: {3}",
                new Object[]{item.getSku(), itemId, quantity, newQty});

        auditService.logAction("STOCK_DECREASE", "InventoryItem", itemId, performedBy,
                String.format("Deducted %d units from SKU %s. Previous: %d, Remaining: %d", quantity, item.getSku(), currentQty, newQty));

        return item;
    }

    /**
     * Internal atomic stock adjustment method.
     * MANDATORY ensures this method MUST execute inside an existing transaction
     * initiated by an orchestrator (such as ShipmentServiceBean.processShipmentDispatch).
     * If called without an active transaction, the container throws TransactionRequiredException.
     */
    @PermitAll
    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public InventoryItem adjustStockInternal(Long itemId, int deltaQuantity) throws InsufficientInventoryException {
        if (itemId == null) {
            throw new IllegalArgumentException("Item ID must not be null.");
        }

        InventoryItem item = em.find(InventoryItem.class, itemId);
        if (item == null) {
            throw new IllegalArgumentException("Inventory item not found for ID: " + itemId);
        }

        int currentQty = item.getQuantity();
        int newQty = currentQty + deltaQuantity;

        if (newQty < 0) {
            LOGGER.log(Level.WARNING, "[InventoryServiceBean] [MANDATORY] Stock adjustment failed: requested deduction of {0} exceeds available {1}",
                    new Object[]{-deltaQuantity, currentQty});
            throw new InsufficientInventoryException(itemId, -deltaQuantity, currentQty);
        }

        item.setQuantity(newQty);
        item.setLastUpdated(LocalDateTime.now());
        em.merge(item);

        LOGGER.log(Level.INFO, "[InventoryServiceBean] [MANDATORY] Internal stock adjusted for SKU {0}. Delta: {1}, New Total: {2}",
                new Object[]{item.getSku(), deltaQuantity, newQty});

        return item;
    }

    /**
     * Checks if current stock has dropped to or below the reorder threshold.
     */
    @PermitAll
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public boolean isReorderLevelReached(Long itemId) {
        InventoryItem item = findInventoryItemById(itemId);
        if (item == null) {
            return false;
        }
        return item.getQuantity() <= item.getReorderLevel();
    }
}
