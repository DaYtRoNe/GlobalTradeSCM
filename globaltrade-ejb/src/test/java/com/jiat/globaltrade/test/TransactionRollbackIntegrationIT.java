package com.jiat.globaltrade.test;

import com.jiat.globaltrade.entity.InventoryItem;
import com.jiat.globaltrade.entity.Shipment;
import com.jiat.globaltrade.entity.enums.ShipmentStatus;
import com.jiat.globaltrade.exception.InsufficientInventoryException;
import com.jiat.globaltrade.service.AuditServiceBean;
import com.jiat.globaltrade.service.InventoryServiceBean;
import com.jiat.globaltrade.service.ShipmentServiceBean;
import jakarta.ejb.EJB;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Phase 7B-1 Integration Test: Verifies EJB Container-Managed Transaction (CMT)
 * rollback semantics and autonomous REQUIRES_NEW audit log commitment.
 *
 * Verifies:
 * 1. An impossible dispatch quantity triggers InsufficientInventoryException (@ApplicationException(rollback = true)).
 * 2. The outer REQUIRED CMT transaction rolls back: inventory quantity and shipment status remain unmodified.
 * 3. The inner REQUIRES_NEW audit transaction commits independently: an audit log record is preserved.
 */
@ExtendWith(ArquillianExtension.class)
public class TransactionRollbackIntegrationIT {

    @Deployment
    public static WebArchive createDeployment() {
        return TestDeployments.createEnterpriseDeployment("transaction-rollback-test.war");
    }

    @EJB
    private AdminTestInvoker adminTestInvoker;

    @EJB
    private InventoryServiceBean inventoryService;

    @EJB
    private ShipmentServiceBean shipmentService;

    @EJB
    private AuditServiceBean auditService;

    @Test
    @DisplayName("Should rollback business transaction on insufficient inventory while committing REQUIRES_NEW audit")
    void shouldRollbackBusinessTransactionAndCommitRequiresNewAudit() {
        assertNotNull(adminTestInvoker, "AdminTestInvoker must be injected by container");
        assertNotNull(inventoryService, "InventoryServiceBean must be injected by container");
        assertNotNull(shipmentService, "ShipmentServiceBean must be injected by container");
        assertNotNull(auditService, "AuditServiceBean must be injected by container");

        // 1. Locate existing test entities (Item #1, Shipment #1)
        Long targetItemId = 1L;
        Long targetShipmentId = 1L;

        InventoryItem initialItem = inventoryService.findInventoryItemById(targetItemId);
        assertNotNull(initialItem, "Inventory item #1 must exist in seeded database");
        int initialQuantity = initialItem.getQuantity();

        Shipment initialShipment = shipmentService.findShipmentById(targetShipmentId);
        assertNotNull(initialShipment, "Shipment #1 must exist in seeded database");
        ShipmentStatus initialStatus = initialShipment.getShipmentStatus();

        long initialAuditCount = auditService.getAuditLogCount();

        // 2. Attempt dispatch with an impossible quantity (e.g. initialQuantity + 999,999)
        int impossibleQuantity = initialQuantity + 999999;
        boolean exceptionCaught = false;

        try {
            adminTestInvoker.processShipmentDispatch(targetShipmentId, targetItemId, impossibleQuantity, "ARQUILLIAN_IT_RUNNER");
            fail("Expected InsufficientInventoryException was not thrown");
        } catch (Exception e) {
            InsufficientInventoryException iie = TestDeployments.findException(e, InsufficientInventoryException.class);
            assertNotNull(iie, "Exception cause chain must contain InsufficientInventoryException. Received: " + e.getClass().getName() + " -> " + e.getMessage());
            exceptionCaught = true;
        }

        assertTrue(exceptionCaught, "InsufficientInventoryException must have been thrown and caught");

        // 3. Verify Business State Rollback (Inventory quantity & Shipment status MUST NOT be mutated)
        InventoryItem itemAfter = inventoryService.findInventoryItemById(targetItemId);
        assertNotNull(itemAfter);
        assertEquals(initialQuantity, itemAfter.getQuantity(),
                "Inventory quantity must remain untouched due to container transaction rollback");

        Shipment shipmentAfter = shipmentService.findShipmentById(targetShipmentId);
        assertNotNull(shipmentAfter);
        assertEquals(initialStatus, shipmentAfter.getShipmentStatus(),
                "Shipment status must remain untouched due to container transaction rollback");

        // 4. Verify REQUIRES_NEW Independent Audit Persistence
        long finalAuditCount = auditService.getAuditLogCount();
        assertTrue(finalAuditCount > initialAuditCount,
                "Audit log count should have increased (Initial: " + initialAuditCount + ", Final: " + finalAuditCount + ") because AuditServiceBean uses REQUIRES_NEW");
    }
}
