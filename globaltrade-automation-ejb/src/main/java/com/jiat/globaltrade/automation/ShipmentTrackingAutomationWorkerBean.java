package com.jiat.globaltrade.automation;

import com.jiat.globaltrade.entity.Shipment;
import com.jiat.globaltrade.entity.enums.ShipmentStatus;
import com.jiat.globaltrade.integration.model.CarrierTrackingPayload;
import com.jiat.globaltrade.integration.service.IntegrationOrchestratorBean;
import com.jiat.globaltrade.service.AuditServiceBean;
import com.jiat.globaltrade.service.ShipmentServiceBean;
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
 * Worker EJB for Automated Scheduled Shipment Telematics Polling.
 * Runs in an isolated REQUIRES_NEW transaction per active shipment.
 * Caller identity (SYSTEM from timer, ADMIN from diagnostic/test) propagates from the entry-point bean.
 * Uses core ShipmentServiceBean and AuditServiceBean via EJB injection.
 */
@Stateless
@TransactionManagement(TransactionManagementType.CONTAINER)
public class ShipmentTrackingAutomationWorkerBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(ShipmentTrackingAutomationWorkerBean.class.getName());

    @EJB
    private ShipmentServiceBean shipmentService;

    @EJB
    private IntegrationOrchestratorBean integrationOrchestrator;

    @EJB
    private AuditServiceBean auditService;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean pollShipmentCarrier(Long shipmentId) {
        if (shipmentId == null) {
            return false;
        }

        Shipment shipment = shipmentService.findShipmentById(shipmentId);
        if (shipment == null || shipment.getShipmentStatus() == ShipmentStatus.DELIVERED
                || shipment.getShipmentStatus() == ShipmentStatus.CANCELLED) {
            return false;
        }

        CarrierTrackingPayload payload = integrationOrchestrator.getCarrierTracking(shipment.getTrackingNumber());
        if (payload != null) {
            LOGGER.log(Level.INFO, "[Automation-Worker] Polled carrier {0} for shipment {1}: status={2}, checkpoint={3}",
                    new Object[]{payload.carrierName(), shipment.getTrackingNumber(), payload.externalStatusCode(), payload.currentCheckpoint()});

            auditService.logAction(
                    "SHIPMENT_TRACKING_POLLED",
                    "Shipment",
                    shipment.getId(),
                    "SYSTEM_AUTOMATION",
                    String.format("Polled carrier telemetry via %s: Status=%s, Checkpoint=%s, Mode=%s",
                            payload.carrierName(), payload.externalStatusCode(), payload.currentCheckpoint(), payload.integrationMode())
            );
            return true;
        }
        return false;
    }
}
