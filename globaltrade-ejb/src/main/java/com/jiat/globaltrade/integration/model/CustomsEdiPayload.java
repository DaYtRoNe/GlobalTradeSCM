package com.jiat.globaltrade.integration.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payload representing national customs EDI clearance authority electronic declaration feedback.
 */
public record CustomsEdiPayload(
        String documentNumber,
        String declarationType,
        String customsAuthority,
        String clearanceStatusCode,
        String entryNumber,
        BigDecimal dutyAssessedUsd,
        LocalDateTime clearanceTimestamp,
        String integrationMode,
        String sourceSystem
) implements Serializable {}
