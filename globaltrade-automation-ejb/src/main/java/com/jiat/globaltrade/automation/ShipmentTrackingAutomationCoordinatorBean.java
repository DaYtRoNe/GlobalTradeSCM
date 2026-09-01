package com.jiat.globaltrade.automation;

import com.jiat.globaltrade.service.ShipmentServiceBean;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Coordinator EJB for Automated Scheduled Shipment Tracking Polling.
 * Runs in NOT_SUPPORTED transaction boundary. Caller identity propagates from entry-point bean.
 */
@Stateless
@TransactionManagement(TransactionManagementType.CONTAINER)
public class ShipmentTrackingAutomationCoordinatorBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(ShipmentTrackingAutomationCoordinatorBean.class.getName());

    @EJB
    private ShipmentServiceBean shipmentService;

    @EJB
    private ShipmentTrackingAutomationWorkerBean workerBean;

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public ShipmentTrackingBatchSummary pollAllActiveShipments() {
        LOGGER.log(Level.INFO, "[Automation-Coordinator] Starting scheduled carrier telemetry polling across active shipments...");

        List<Long> activeShipmentIds = shipmentService.findActiveShipmentIds();

        int total = activeShipmentIds != null ? activeShipmentIds.size() : 0;
        int success = 0;
        int failed = 0;

        if (activeShipmentIds != null) {
            for (Long shipmentId : activeShipmentIds) {
                try {
                    boolean result = workerBean.pollShipmentCarrier(shipmentId);
                    if (result) {
                        success++;
                    } else {
                        failed++;
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "[Automation-Coordinator] Failed polling shipment #{0}: {1}",
                            new Object[]{shipmentId, e.getMessage()});
                    failed++;
                }
            }
        }

        LOGGER.log(Level.INFO, "[Automation-Coordinator] Completed shipment tracking polling: total={0}, success={1}, failed={2}",
                new Object[]{total, success, failed});

        return new ShipmentTrackingBatchSummary(total, success, failed);
    }

    public record ShipmentTrackingBatchSummary(int totalEvaluated, int successfulPolls, int failedPolls) implements Serializable {}
}
