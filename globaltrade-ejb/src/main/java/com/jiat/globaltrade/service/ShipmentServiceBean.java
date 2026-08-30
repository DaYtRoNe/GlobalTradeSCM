package com.jiat.globaltrade.service;

import com.jiat.globaltrade.entity.InventoryItem;
import com.jiat.globaltrade.entity.Shipment;
import com.jiat.globaltrade.entity.Vendor;
import com.jiat.globaltrade.entity.enums.ShipmentStatus;
import com.jiat.globaltrade.exception.InsufficientInventoryException;
import com.jiat.globaltrade.interceptor.BusinessAuditInterceptor;
import com.jiat.globaltrade.interceptor.BusinessValidationInterceptor;
import com.jiat.globaltrade.interceptor.PerformanceMonitoringInterceptor;
import com.jiat.globaltrade.interceptor.TradeComplianceInterceptor;
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
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Core business service for shipment operations and transactional dispatch orchestration.
 * Demonstrates CMT transaction orchestration, Interceptor Chaining, and Method-Level RBAC.
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
public class ShipmentServiceBean {

    private static final Logger LOGGER = Logger.getLogger(ShipmentServiceBean.class.getName());

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @EJB
    private InventoryServiceBean inventoryService;

    @EJB
    private AuditServiceBean auditService;

    /**
     * Read-only lookup for a shipment by ID.
     */
    @PermitAll
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Shipment findShipmentById(Long id) {
        if (id == null) {
            return null;
        }
        return em.find(Shipment.class, id);
    }

    /**
     * Read-only lookup for all shipments.
     */
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.LOGISTICS_COORDINATOR})
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Shipment> findAllShipments() {
        return em.createQuery("SELECT s FROM Shipment s ORDER BY s.createdAt DESC", Shipment.class)
                .getResultList();
    }

    /**
     * Creates a new shipment associated with a specific vendor.
     * REQUIRED ensures atomic persistence.
     */
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.LOGISTICS_COORDINATOR})
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Shipment createShipment(Shipment shipment, Long vendorId, String performedBy) {
        if (shipment == null || vendorId == null) {
            throw new IllegalArgumentException("Shipment and vendor ID must not be null.");
        }

        Vendor vendor = em.find(Vendor.class, vendorId);
        if (vendor == null) {
            throw new IllegalArgumentException("Vendor not found for ID: " + vendorId);
        }

        shipment.setVendor(vendor);
        if (shipment.getCreatedAt() == null) {
            shipment.setCreatedAt(LocalDateTime.now());
        }
        if (shipment.getShipmentStatus() == null) {
            shipment.setShipmentStatus(ShipmentStatus.PENDING);
        }

        em.persist(shipment);
        LOGGER.log(Level.INFO, "[ShipmentServiceBean] [REQUIRED] Created shipment {0} for vendor {1}",
                new Object[]{shipment.getTrackingNumber(), vendor.getCompanyName()});

        auditService.logAction("CREATE_SHIPMENT", "Shipment", shipment.getId(), performedBy,
                String.format("Tracking: %s, Origin: %s, Destination: %s",
                        shipment.getTrackingNumber(), shipment.getOrigin(), shipment.getDestination()));

        return shipment;
    }

    /**
     * Updates the status of an existing shipment.
     */
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.LOGISTICS_COORDINATOR})
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Shipment updateShipmentStatus(Long shipmentId, ShipmentStatus newStatus, String performedBy) {
        if (shipmentId == null || newStatus == null) {
            throw new IllegalArgumentException("Shipment ID and status must not be null.");
        }

        Shipment shipment = em.find(Shipment.class, shipmentId);
        if (shipment == null) {
            LOGGER.log(Level.WARNING, "[ShipmentServiceBean] Shipment not found for ID: {0}", shipmentId);
            return null;
        }

        ShipmentStatus oldStatus = shipment.getShipmentStatus();
        shipment.setShipmentStatus(newStatus);
        shipment.setUpdatedAt(LocalDateTime.now());
        em.merge(shipment);

        LOGGER.log(Level.INFO, "[ShipmentServiceBean] [REQUIRED] Shipment {0} status updated from {1} to {2}",
                new Object[]{shipmentId, oldStatus, newStatus});

        auditService.logAction("UPDATE_SHIPMENT_STATUS", "Shipment", shipmentId, performedBy,
                String.format("Status changed from %s to %s", oldStatus, newStatus));

        return shipment;
    }

    /**
     * Marks a shipment as delivered and records the actual delivery date.
     */
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.LOGISTICS_COORDINATOR, SecurityRoles.WAREHOUSE_MANAGER})
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Shipment markShipmentDelivered(Long shipmentId, LocalDate actualDeliveryDate, String performedBy) {
        if (shipmentId == null) {
            throw new IllegalArgumentException("Shipment ID must not be null.");
        }

        Shipment shipment = em.find(Shipment.class, shipmentId);
        if (shipment == null) {
            LOGGER.log(Level.WARNING, "[ShipmentServiceBean] Shipment not found for ID: {0}", shipmentId);
            return null;
        }

        shipment.setShipmentStatus(ShipmentStatus.DELIVERED);
        shipment.setActualDeliveryDate(actualDeliveryDate != null ? actualDeliveryDate : LocalDate.now());
        shipment.setUpdatedAt(LocalDateTime.now());
        em.merge(shipment);

        LOGGER.log(Level.INFO, "[ShipmentServiceBean] [REQUIRED] Shipment {0} marked DELIVERED", shipmentId);

        auditService.logAction("DELIVER_SHIPMENT", "Shipment", shipmentId, performedBy,
                String.format("Delivered on %s", shipment.getActualDeliveryDate()));

        return shipment;
    }

    /**
     * Multi-step atomic transaction orchestrating shipment dispatch:
     * Demonstrates Complete Interceptor Chaining Order:
     * 1. BusinessValidationInterceptor (Input parameter validation)
     * 2. TradeComplianceInterceptor (Regulatory & operator authorization check)
     * 3. PerformanceMonitoringInterceptor (Execution timing measurement)
     * 4. BusinessAuditInterceptor (Autonomous business invocation auditing)
     * 5. Business logic execution inside CMT REQUIRED transaction
     */
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.LOGISTICS_COORDINATOR, SecurityRoles.WAREHOUSE_MANAGER})
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @Interceptors({
            BusinessValidationInterceptor.class,
            TradeComplianceInterceptor.class,
            PerformanceMonitoringInterceptor.class,
            BusinessAuditInterceptor.class
    })
    public Shipment processShipmentDispatch(Long shipmentId, Long inventoryItemId, int dispatchQuantity, String performedBy)
            throws InsufficientInventoryException {

        LOGGER.log(Level.INFO, "[ShipmentServiceBean] [REQUIRED] Beginning multi-step dispatch transaction for Shipment #{0}, Item #{1}, Qty={2}",
                new Object[]{shipmentId, inventoryItemId, dispatchQuantity});

        if (shipmentId == null || inventoryItemId == null || dispatchQuantity <= 0) {
            throw new IllegalArgumentException("Invalid dispatch parameters.");
        }

        Shipment shipment = em.find(Shipment.class, shipmentId);
        if (shipment == null) {
            throw new IllegalArgumentException("Shipment not found for ID: " + shipmentId);
        }

        // Step 1: Pre-dispatch state check
        ShipmentStatus initialStatus = shipment.getShipmentStatus();

        try {
            // Step 2: Perform stock deduction via MANDATORY transaction attribute
            // If this throws InsufficientInventoryException, the container rolls back the entire transaction.
            InventoryItem updatedItem = inventoryService.adjustStockInternal(inventoryItemId, -dispatchQuantity);

            // Step 3: Transition shipment to IN_TRANSIT
            shipment.setShipmentStatus(ShipmentStatus.IN_TRANSIT);
            shipment.setUpdatedAt(LocalDateTime.now());
            em.merge(shipment);

            LOGGER.log(Level.INFO, "[ShipmentServiceBean] [REQUIRED] Dispatch successful: Shipment #{0} -> IN_TRANSIT, SKU {1} remaining={2}",
                    new Object[]{shipmentId, updatedItem.getSku(), updatedItem.getQuantity()});

            // Step 4: Audit record in independent transaction (REQUIRES_NEW)
            auditService.logAction("SHIPMENT_DISPATCH_SUCCESS", "Shipment", shipmentId, performedBy,
                    String.format("Dispatched %d units of SKU %s. Shipment status: %s -> %s",
                            dispatchQuantity, updatedItem.getSku(), initialStatus, ShipmentStatus.IN_TRANSIT));

            return shipment;
        } catch (InsufficientInventoryException e) {
            LOGGER.log(Level.WARNING, "[ShipmentServiceBean] [REQUIRED] Dispatch failed due to insufficient stock: {0}. Logging failure via REQUIRES_NEW before rollback.",
                    e.getMessage());

            // Audit the failed attempt independently via REQUIRES_NEW
            // This audit record commits in its own transaction even though the outer transaction rolls back!
            auditService.logAction("DISPATCH_FAILED_INSUFFICIENT_STOCK", "Shipment", shipmentId, performedBy,
                    String.format("Dispatch of %d units failed for item #%d: %s. Main transaction rolling back.",
                            dispatchQuantity, inventoryItemId, e.getMessage()));

            // Rethrow to trigger container rollback of outer REQUIRED transaction via @ApplicationException(rollback = true)
            throw e;
        }
    }
}
