package com.jiat.globaltrade.integration.gateway;

import com.jiat.globaltrade.integration.model.CustomsEdiPayload;

/**
 * Enterprise Integration Gateway interface for National Customs Authorities & EDI Clearance Platforms.
 */
public interface CustomsSystemGateway {

    CustomsEdiPayload queryClearanceStatus(String documentNumber);

    CustomsEdiPayload submitDeclaration(String documentNumber, String declarationType, String originCountry, String destinationCountry);
}
