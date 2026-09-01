package com.jiat.globaltrade.web.dto;

import java.io.Serializable;

public class TradeAgreementRuleResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String agreementCode;
    private String agreementName;
    private String originCountry;
    private String destinationCountry;
    private String documentTypeRequired;
    private boolean active;
    private String description;

    public TradeAgreementRuleResponse() {
    }

    public TradeAgreementRuleResponse(Long id, String agreementCode, String agreementName,
                                      String originCountry, String destinationCountry,
                                      String documentTypeRequired, boolean active, String description) {
        this.id = id;
        this.agreementCode = agreementCode;
        this.agreementName = agreementName;
        this.originCountry = originCountry;
        this.destinationCountry = destinationCountry;
        this.documentTypeRequired = documentTypeRequired;
        this.active = active;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAgreementCode() {
        return agreementCode;
    }

    public void setAgreementCode(String agreementCode) {
        this.agreementCode = agreementCode;
    }

    public String getAgreementName() {
        return agreementName;
    }

    public void setAgreementName(String agreementName) {
        this.agreementName = agreementName;
    }

    public String getOriginCountry() {
        return originCountry;
    }

    public void setOriginCountry(String originCountry) {
        this.originCountry = originCountry;
    }

    public String getDestinationCountry() {
        return destinationCountry;
    }

    public void setDestinationCountry(String destinationCountry) {
        this.destinationCountry = destinationCountry;
    }

    public String getDocumentTypeRequired() {
        return documentTypeRequired;
    }

    public void setDocumentTypeRequired(String documentTypeRequired) {
        this.documentTypeRequired = documentTypeRequired;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
