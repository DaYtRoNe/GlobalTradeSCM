package com.jiat.globaltrade.web.dto;

import com.jiat.globaltrade.entity.SupplyChainAlert;
import com.jiat.globaltrade.entity.enums.SupplyChainAlertStatus;
import com.jiat.globaltrade.entity.enums.SupplyChainAlertType;
import java.io.Serializable;
import java.time.format.DateTimeFormatter;

/**
 * Public REST DTO representing a persistent supply chain alert.
 * Excludes internal stack traces, passwords, or unrelated tenant data.
 */
public class SupplyChainAlertResponse implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    private Long id;
    private String alertKey;
    private SupplyChainAlertType alertType;
    private SupplyChainAlertStatus alertStatus;
    private String entityType;
    private Long entityId;
    private String message;
    private String detectedAt;
    private String lastDetectedAt;
    private String acknowledgedAt;
    private String resolvedAt;
    private String acknowledgedBy;

    public SupplyChainAlertResponse() {
    }

    public static SupplyChainAlertResponse fromEntity(SupplyChainAlert alert) {
        if (alert == null) {
            return null;
        }

        SupplyChainAlertResponse dto = new SupplyChainAlertResponse();
        dto.setId(alert.getId());
        dto.setAlertKey(alert.getAlertKey());
        dto.setAlertType(alert.getAlertType());
        dto.setAlertStatus(alert.getAlertStatus());
        dto.setEntityType(alert.getEntityType());
        dto.setEntityId(alert.getEntityId());
        dto.setMessage(alert.getMessage());

        if (alert.getDetectedAt() != null) {
            dto.setDetectedAt(alert.getDetectedAt().format(FORMATTER));
        }
        if (alert.getLastDetectedAt() != null) {
            dto.setLastDetectedAt(alert.getLastDetectedAt().format(FORMATTER));
        }
        if (alert.getAcknowledgedAt() != null) {
            dto.setAcknowledgedAt(alert.getAcknowledgedAt().format(FORMATTER));
        }
        if (alert.getResolvedAt() != null) {
            dto.setResolvedAt(alert.getResolvedAt().format(FORMATTER));
        }

        dto.setAcknowledgedBy(alert.getAcknowledgedBy());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAlertKey() {
        return alertKey;
    }

    public void setAlertKey(String alertKey) {
        this.alertKey = alertKey;
    }

    public SupplyChainAlertType getAlertType() {
        return alertType;
    }

    public void setAlertType(SupplyChainAlertType alertType) {
        this.alertType = alertType;
    }

    public SupplyChainAlertStatus getAlertStatus() {
        return alertStatus;
    }

    public void setAlertStatus(SupplyChainAlertStatus alertStatus) {
        this.alertStatus = alertStatus;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDetectedAt() {
        return detectedAt;
    }

    public void setDetectedAt(String detectedAt) {
        this.detectedAt = detectedAt;
    }

    public String getLastDetectedAt() {
        return lastDetectedAt;
    }

    public void setLastDetectedAt(String lastDetectedAt) {
        this.lastDetectedAt = lastDetectedAt;
    }

    public String getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public void setAcknowledgedAt(String acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    public String getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(String resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getAcknowledgedBy() {
        return acknowledgedBy;
    }

    public void setAcknowledgedBy(String acknowledgedBy) {
        this.acknowledgedBy = acknowledgedBy;
    }
}
