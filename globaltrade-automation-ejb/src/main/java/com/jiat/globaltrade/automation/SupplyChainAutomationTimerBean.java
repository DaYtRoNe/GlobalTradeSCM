package com.jiat.globaltrade.automation;

import jakarta.ejb.EJB;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dedicated Persistent EJB Timer for Scheduled Supply Chain Automation Workflows.
 * Triggers automated shipment telematics polling, inventory replenishment order dispatches,
 * and customs clearance status synchronization.
 */
@Singleton
@Startup
public class SupplyChainAutomationTimerBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(SupplyChainAutomationTimerBean.class.getName());

    @EJB
    private ShipmentTrackingAutomationCoordinatorBean shipmentTrackingCoordinator;

    @EJB
    private InventoryReplenishmentAutomationCoordinatorBean inventoryReplenishmentCoordinator;

    @EJB
    private CustomsDocumentationAutomationCoordinatorBean customsDocumentationCoordinator;

    /**
     * Runs periodically every 15 minutes as a persistent container-managed timer.
     */
    @Schedule(hour = "*", minute = "*/15", second = "0", persistent = true, info = "DeclarativeSupplyChainAutomationTimer")
    public void executeScheduledAutomationCycle() {
        LOGGER.log(Level.INFO, "[Automation-Timer] ==========================================================================");
        LOGGER.log(Level.INFO, "[Automation-Timer] Triggering Scheduled Supply Chain Automation Engine Cycle...");
        LOGGER.log(Level.INFO, "[Automation-Timer] ==========================================================================");

        try {
            // 1. Automated Shipment Telematics Polling
            ShipmentTrackingAutomationCoordinatorBean.ShipmentTrackingBatchSummary trackingSummary =
                    shipmentTrackingCoordinator.pollAllActiveShipments();
            LOGGER.log(Level.INFO, "[Automation-Timer] Tracking Polling: {0}/{1} active shipments processed successfully.",
                    new Object[]{trackingSummary.successfulPolls(), trackingSummary.totalEvaluated()});

            // 2. Automated Inventory Replenishment Dispatch
            InventoryReplenishmentAutomationCoordinatorBean.ReplenishmentBatchSummary replenishmentSummary =
                    inventoryReplenishmentCoordinator.evaluateAndReplenishAllItems();
            LOGGER.log(Level.INFO, "[Automation-Timer] Replenishment: {0} orders placed, {1} items evaluated.",
                    new Object[]{replenishmentSummary.replenishmentOrdersPlaced(), replenishmentSummary.totalItems()});

            // 3. Automated Customs Documentation Status Synchronization
            CustomsDocumentationAutomationCoordinatorBean.CustomsPollingBatchSummary customsSummary =
                    customsDocumentationCoordinator.pollAllPendingDocuments();
            LOGGER.log(Level.INFO, "[Automation-Timer] Customs Polling: {0}/{1} pending documents processed successfully.",
                    new Object[]{customsSummary.successfulPolls(), customsSummary.totalEvaluated()});

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[Automation-Timer] Unhandled error during scheduled automation cycle: " + e.getMessage(), e);
        }

        LOGGER.log(Level.INFO, "[Automation-Timer] Scheduled Supply Chain Automation Cycle completed.");
    }
}
