package com.jiat.globaltrade.integration.adapter;

import com.jiat.globaltrade.integration.gateway.ShippingCarrierGateway;
import com.jiat.globaltrade.integration.model.CarrierTrackingPayload;
import jakarta.ejb.Stateless;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Enterprise Simulated Adapter for Shipping Carrier Telematics Integration.
 * Demonstrates external logistics carrier telemetry boundaries with realistic simulated events.
 */
@Stateless
public class SimulatedCarrierGatewayBean implements ShippingCarrierGateway, Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(SimulatedCarrierGatewayBean.class.getName());

    public static final String INTEGRATION_MODE = "SIMULATED";
    public static final String SOURCE_SYSTEM = "SIMULATED_CARRIER_GATEWAY_V1";

    @Override
    public CarrierTrackingPayload fetchCarrierTracking(String trackingNumber) {
        LOGGER.log(Level.INFO, "[SimulatedCarrierGateway] Polling external carrier telematics for: {0}", trackingNumber);

        String carrierCode = trackingNumber != null && trackingNumber.contains("AIR") ? "NAC" : "PME";
        String carrierName = "NAC".equals(carrierCode) ? "Nippon Air Cargo" : "Pacific Maritime Express";
        String transportMode = "NAC".equals(carrierCode) ? "AIR" : "SEA";
        String checkpoint = "Singapore Port Terminal - Gate 4 Inbound Checkpoint";
        String status = "IN_TRANSIT_ON_SCHEDULE";
        String window = "Next 48 Hours";

        return new CarrierTrackingPayload(
                trackingNumber,
                carrierName,
                carrierCode,
                transportMode,
                status,
                checkpoint,
                window,
                LocalDateTime.now(),
                INTEGRATION_MODE,
                SOURCE_SYSTEM
        );
    }

    @Override
    public boolean registerShipmentWithCarrier(String trackingNumber, String carrierCode, String origin, String destination) {
        LOGGER.log(Level.INFO, "[SimulatedCarrierGateway] Transmitted booking manifest for {0} to carrier {1} ({2} -> {3})",
                new Object[]{trackingNumber, carrierCode, origin, destination});
        return true;
    }
}
