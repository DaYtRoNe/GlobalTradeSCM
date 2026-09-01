package com.jiat.globaltrade.integration.gateway;

import com.jiat.globaltrade.integration.model.CarrierTrackingPayload;

/**
 * Enterprise Integration Gateway interface for Freight & Shipping Carrier systems.
 */
public interface ShippingCarrierGateway {

    CarrierTrackingPayload fetchCarrierTracking(String trackingNumber);

    boolean registerShipmentWithCarrier(String trackingNumber, String carrierCode, String origin, String destination);
}
