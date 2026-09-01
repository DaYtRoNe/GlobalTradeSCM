package com.jiat.globaltrade.web.dto;

import java.io.Serializable;
import java.util.List;

public class TradeComplianceResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long shipmentId;
    private String trackingNumber;
    private String origin;
    private String destination;
    private boolean compliant;
    private List<String> applicableAgreements;
    private List<String> satisfiedDocuments;
    private List<String> missingDocuments;
    private String rationale;

    public TradeComplianceResponse() {
    }

    public TradeComplianceResponse(Long shipmentId, String trackingNumber, String origin,
                                   String destination, boolean compliant,
                                   List<String> applicableAgreements,
                                   List<String> satisfiedDocuments,
                                   List<String> missingDocuments, String rationale) {
        this.shipmentId = shipmentId;
        this.trackingNumber = trackingNumber;
        this.origin = origin;
        this.destination = destination;
        this.compliant = compliant;
        this.applicableAgreements = applicableAgreements;
        this.satisfiedDocuments = satisfiedDocuments;
        this.missingDocuments = missingDocuments;
        this.rationale = rationale;
    }

    public Long getShipmentId() {
        return shipmentId;
    }

    public void setShipmentId(Long shipmentId) {
        this.shipmentId = shipmentId;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public boolean isCompliant() {
        return compliant;
    }

    public void setCompliant(boolean compliant) {
        this.compliant = compliant;
    }

    public List<String> getApplicableAgreements() {
        return applicableAgreements;
    }

    public void setApplicableAgreements(List<String> applicableAgreements) {
        this.applicableAgreements = applicableAgreements;
    }

    public List<String> getSatisfiedDocuments() {
        return satisfiedDocuments;
    }

    public void setSatisfiedDocuments(List<String> satisfiedDocuments) {
        this.satisfiedDocuments = satisfiedDocuments;
    }

    public List<String> getMissingDocuments() {
        return missingDocuments;
    }

    public void setMissingDocuments(List<String> missingDocuments) {
        this.missingDocuments = missingDocuments;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }
}
