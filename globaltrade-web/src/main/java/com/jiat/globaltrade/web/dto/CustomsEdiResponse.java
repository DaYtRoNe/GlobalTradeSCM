package com.jiat.globaltrade.web.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public class CustomsEdiResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String documentNumber;
    private String declarationType;
    private String customsAuthority;
    private String clearanceStatusCode;
    private String entryNumber;
    private BigDecimal dutyAssessedUsd;
    private String clearanceTimestamp;
    private String integrationMode;
    private String sourceSystem;

    public CustomsEdiResponse() {
    }

    public CustomsEdiResponse(String documentNumber, String declarationType, String customsAuthority,
                              String clearanceStatusCode, String entryNumber, BigDecimal dutyAssessedUsd,
                              String clearanceTimestamp, String integrationMode, String sourceSystem) {
        this.documentNumber = documentNumber;
        this.declarationType = declarationType;
        this.customsAuthority = customsAuthority;
        this.clearanceStatusCode = clearanceStatusCode;
        this.entryNumber = entryNumber;
        this.dutyAssessedUsd = dutyAssessedUsd;
        this.clearanceTimestamp = clearanceTimestamp;
        this.integrationMode = integrationMode;
        this.sourceSystem = sourceSystem;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public String getDeclarationType() {
        return declarationType;
    }

    public void setDeclarationType(String declarationType) {
        this.declarationType = declarationType;
    }

    public String getCustomsAuthority() {
        return customsAuthority;
    }

    public void setCustomsAuthority(String customsAuthority) {
        this.customsAuthority = customsAuthority;
    }

    public String getClearanceStatusCode() {
        return clearanceStatusCode;
    }

    public void setClearanceStatusCode(String clearanceStatusCode) {
        this.clearanceStatusCode = clearanceStatusCode;
    }

    public String getEntryNumber() {
        return entryNumber;
    }

    public void setEntryNumber(String entryNumber) {
        this.entryNumber = entryNumber;
    }

    public BigDecimal getDutyAssessedUsd() {
        return dutyAssessedUsd;
    }

    public void setDutyAssessedUsd(BigDecimal dutyAssessedUsd) {
        this.dutyAssessedUsd = dutyAssessedUsd;
    }

    public String getClearanceTimestamp() {
        return clearanceTimestamp;
    }

    public void setClearanceTimestamp(String clearanceTimestamp) {
        this.clearanceTimestamp = clearanceTimestamp;
    }

    public String getIntegrationMode() {
        return integrationMode;
    }

    public void setIntegrationMode(String integrationMode) {
        this.integrationMode = integrationMode;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }
}
