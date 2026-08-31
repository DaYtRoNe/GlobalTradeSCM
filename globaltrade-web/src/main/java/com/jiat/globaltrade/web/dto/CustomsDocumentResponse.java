package com.jiat.globaltrade.web.dto;

import com.jiat.globaltrade.entity.CustomsDocument;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Safe, detached REST response DTO for CustomsDocument entities.
 */
public class CustomsDocumentResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String documentNumber;
    private String documentType;
    private String status;
    private LocalDate submissionDeadline;
    private Long shipmentId;
    private String shipmentTrackingNumber;
    private LocalDateTime createdAt;

    public CustomsDocumentResponse() {
    }

    public static CustomsDocumentResponse fromEntity(CustomsDocument doc) {
        if (doc == null) {
            return null;
        }
        CustomsDocumentResponse dto = new CustomsDocumentResponse();
        dto.setId(doc.getId());
        dto.setDocumentNumber(doc.getDocumentNumber());
        dto.setDocumentType(doc.getDocumentType() != null ? doc.getDocumentType().name() : null);
        dto.setStatus(doc.getStatus() != null ? doc.getStatus().name() : null);
        dto.setSubmissionDeadline(doc.getSubmissionDeadline());
        dto.setCreatedAt(doc.getCreatedAt());
        if (doc.getShipment() != null) {
            dto.setShipmentId(doc.getShipment().getId());
            dto.setShipmentTrackingNumber(doc.getShipment().getTrackingNumber());
        }
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getSubmissionDeadline() {
        return submissionDeadline;
    }

    public void setSubmissionDeadline(LocalDate submissionDeadline) {
        this.submissionDeadline = submissionDeadline;
    }

    public Long getShipmentId() {
        return shipmentId;
    }

    public void setShipmentId(Long shipmentId) {
        this.shipmentId = shipmentId;
    }

    public String getShipmentTrackingNumber() {
        return shipmentTrackingNumber;
    }

    public void setShipmentTrackingNumber(String shipmentTrackingNumber) {
        this.shipmentTrackingNumber = shipmentTrackingNumber;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
