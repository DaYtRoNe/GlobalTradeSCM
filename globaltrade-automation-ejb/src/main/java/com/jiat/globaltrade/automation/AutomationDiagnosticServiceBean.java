package com.jiat.globaltrade.automation;

import com.jiat.globaltrade.security.SecurityRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Diagnostic and Administrative Service for manually triggering and testing
 * Supply Chain Automation workflows.
 */
@Stateless
@TransactionManagement(TransactionManagementType.CONTAINER)
public class AutomationDiagnosticServiceBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(AutomationDiagnosticServiceBean.class.getName());

    @EJB
    private ShipmentTrackingAutomationCoordinatorBean shipmentTrackingCoordinator;

    @EJB
    private InventoryReplenishmentAutomationCoordinatorBean inventoryReplenishmentCoordinator;

    @EJB
    private CustomsDocumentationAutomationCoordinatorBean customsDocumentationCoordinator;

    @EJB
    private ShipmentTrackingAutomationWorkerBean trackingWorker;

    @EJB
    private InventoryReplenishmentAutomationWorkerBean replenishmentWorker;

    @EJB
    private CustomsDocumentationAutomationWorkerBean customsWorker;

    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.SYSTEM})
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public FullAutomationCycleSummary runFullAutomationCycle(String triggerSource) {
        LOGGER.log(Level.INFO, "[AutomationDiagnostic] Manual trigger requested by {0}...", triggerSource);

        ShipmentTrackingAutomationCoordinatorBean.ShipmentTrackingBatchSummary tracking =
                shipmentTrackingCoordinator.pollAllActiveShipments();

        InventoryReplenishmentAutomationCoordinatorBean.ReplenishmentBatchSummary replenishment =
                inventoryReplenishmentCoordinator.evaluateAndReplenishAllItems();

        CustomsDocumentationAutomationCoordinatorBean.CustomsPollingBatchSummary customs =
                customsDocumentationCoordinator.pollAllPendingDocuments();

        return new FullAutomationCycleSummary(
                "SUCCESS",
                triggerSource,
                LocalDateTime.now(),
                tracking.successfulPolls(),
                replenishment.replenishmentOrdersPlaced(),
                customs.successfulPolls()
        );
    }

    @RolesAllowed(SecurityRoles.ADMIN)
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public boolean pollSingleShipment(Long shipmentId) {
        return trackingWorker.pollShipmentCarrier(shipmentId);
    }

    @RolesAllowed(SecurityRoles.ADMIN)
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public InventoryReplenishmentAutomationWorkerBean.ReplenishmentEvaluationResult evaluateSingleInventoryItem(Long itemId) {
        return replenishmentWorker.evaluateAndReplenish(itemId);
    }

    @RolesAllowed(SecurityRoles.ADMIN)
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public boolean pollSingleCustomsDocument(Long documentId) {
        return customsWorker.pollCustomsDocumentStatus(documentId);
    }

    public record FullAutomationCycleSummary(
            String status,
            String triggerSource,
            LocalDateTime timestamp,
            int shipmentsPolled,
            int replenishmentOrdersDispatched,
            int customsDocumentsPolled
    ) implements Serializable {}
}
