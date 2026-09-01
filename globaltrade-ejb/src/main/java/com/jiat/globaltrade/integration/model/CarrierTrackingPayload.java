package com.jiat.globaltrade.integration.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Payload representing external shipping carrier telematics and tracking events.
 */
public record CarrierTrackingPayload(
        String trackingNumber,
        String carrierName,
        String carrierCode,
        String transportMode,
        String externalStatusCode,
        String currentCheckpoint,
        String estimatedDeliveryWindow,
        LocalDateTime lastEventTimestamp,
        String integrationMode,
        String sourceSystem
) implements Serializable {}
