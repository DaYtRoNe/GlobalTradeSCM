package com.jiat.globaltrade.integration.adapter;

import com.jiat.globaltrade.integration.gateway.CustomsSystemGateway;
import com.jiat.globaltrade.integration.model.CustomsEdiPayload;
import jakarta.ejb.Stateless;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Enterprise Simulated Adapter for National Customs EDI Authority Integration.
 * Demonstrates cross-border regulatory electronic filing acknowledgment and customs clearance.
 */
@Stateless
public class SimulatedCustomsGatewayBean implements CustomsSystemGateway, Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(SimulatedCustomsGatewayBean.class.getName());

    public static final String INTEGRATION_MODE = "SIMULATED";
    public static final String SOURCE_SYSTEM = "SIMULATED_CUSTOMS_EDI_GATEWAY_V1";

    @Override
    public CustomsEdiPayload queryClearanceStatus(String documentNumber) {
        LOGGER.log(Level.INFO, "[SimulatedCustomsGateway] Inquiring national customs clearance status for: {0}", documentNumber);

        return new CustomsEdiPayload(
                documentNumber,
                "IMPORT_CLEARANCE_EDIFACT_CUSDEC",
                "Singapore Customs & Border Protection Authority",
                "CLEARED_ASSESSMENT_COMPLETE",
                "ENT-SGP-2026-" + Math.abs(documentNumber != null ? documentNumber.hashCode() % 10000 : 1001),
                new BigDecimal("420.50"),
                LocalDateTime.now(),
                INTEGRATION_MODE,
                SOURCE_SYSTEM
        );
    }

    @Override
    public CustomsEdiPayload submitDeclaration(String documentNumber, String declarationType, String originCountry, String destinationCountry) {
        LOGGER.log(Level.INFO, "[SimulatedCustomsGateway] Electronic declaration submitted: {0} ({1} -> {2})",
                new Object[]{documentNumber, originCountry, destinationCountry});

        return new CustomsEdiPayload(
                documentNumber,
                declarationType != null ? declarationType : "STANDARD_IMPORT_DECLARATION",
                "National Single Window Customs Portal",
                "ELECTRONIC_FILING_ACKNOWLEDGED",
                "ENT-NSW-" + System.currentTimeMillis() % 100000,
                new BigDecimal("150.00"),
                LocalDateTime.now(),
                INTEGRATION_MODE,
                SOURCE_SYSTEM
        );
    }
}
