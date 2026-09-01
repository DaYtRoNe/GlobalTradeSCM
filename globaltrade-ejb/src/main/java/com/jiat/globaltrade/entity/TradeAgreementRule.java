package com.jiat.globaltrade.entity;

import com.jiat.globaltrade.entity.enums.CustomsDocumentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

/**
 * Entity representing international bilateral and multilateral trade agreement statutory rules
 * (e.g. USMCA, CPTPP, EU-Singapore FTA, Japan-Singapore EPA).
 * Enforces required documentation types for cross-border international corridors.
 */
@Entity
@Table(name = "trade_agreement_rules")
public class TradeAgreementRule implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agreement_code", nullable = false, length = 50)
    private String agreementCode;

    @Column(name = "agreement_name", nullable = false, length = 150)
    private String agreementName;

    @Column(name = "origin_country", nullable = false, length = 100)
    private String originCountry;

    @Column(name = "destination_country", nullable = false, length = 100)
    private String destinationCountry;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type_required", nullable = false, length = 50)
    private CustomsDocumentType documentTypeRequired;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "description", length = 300)
    private String description;

    public TradeAgreementRule() {
    }

    public TradeAgreementRule(String agreementCode, String agreementName, String originCountry,
                              String destinationCountry, CustomsDocumentType documentTypeRequired,
                              boolean active, String description) {
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

    public CustomsDocumentType getDocumentTypeRequired() {
        return documentTypeRequired;
    }

    public void setDocumentTypeRequired(CustomsDocumentType documentTypeRequired) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TradeAgreementRule that)) return false;
        return Objects.equals(id, that.id) || (Objects.equals(agreementCode, that.agreementCode)
                && Objects.equals(originCountry, that.originCountry)
                && Objects.equals(destinationCountry, that.destinationCountry));
    }

    @Override
    public int hashCode() {
        return Objects.hash(agreementCode, originCountry, destinationCountry);
    }
}
