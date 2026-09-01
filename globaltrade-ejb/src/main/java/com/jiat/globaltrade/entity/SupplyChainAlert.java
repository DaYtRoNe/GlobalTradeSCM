package com.jiat.globaltrade.entity;

import com.jiat.globaltrade.entity.enums.SupplyChainAlertStatus;
import com.jiat.globaltrade.entity.enums.SupplyChainAlertType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Persistent Supply Chain Alert Entity.
 * Represents an actionable operational condition identified by automated monitoring timers.
 * Prevents duplicate alerts across consecutive timer cycles using a unique alert_key.
 */
@Entity
@Table(name = "supply_chain_alerts")
public class SupplyChainAlert implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alert_key", nullable = false, unique = true, length = 100)
    private String alertKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 50)
    private SupplyChainAlertType alertType;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_status", nullable = false, length = 30)
    private SupplyChainAlertStatus alertStatus = SupplyChainAlertStatus.OPEN;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    @Column(name = "detected_at", nullable = false)
    private LocalDateTime detectedAt;

    @Column(name = "last_detected_at", nullable = false)
    private LocalDateTime lastDetectedAt;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "acknowledged_by", length = 100)
    private String acknowledgedBy;

    public SupplyChainAlert() {
    }

    public SupplyChainAlert(String alertKey, SupplyChainAlertType alertType, String entityType, Long entityId, String message) {
        this.alertKey = alertKey;
        this.alertType = alertType;
        this.entityType = entityType;
        this.entityId = entityId;
        this.message = message;
        this.alertStatus = SupplyChainAlertStatus.OPEN;
        LocalDateTime now = LocalDateTime.now();
        this.detectedAt = now;
        this.lastDetectedAt = now;
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (this.detectedAt == null) {
            this.detectedAt = now;
        }
        if (this.lastDetectedAt == null) {
            this.lastDetectedAt = now;
        }
        if (this.alertStatus == null) {
            this.alertStatus = SupplyChainAlertStatus.OPEN;
        }
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

    public LocalDateTime getDetectedAt() {
        return detectedAt;
    }

    public void setDetectedAt(LocalDateTime detectedAt) {
        this.detectedAt = detectedAt;
    }

    public LocalDateTime getLastDetectedAt() {
        return lastDetectedAt;
    }

    public void setLastDetectedAt(LocalDateTime lastDetectedAt) {
        this.lastDetectedAt = lastDetectedAt;
    }

    public LocalDateTime getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public void setAcknowledgedAt(LocalDateTime acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getAcknowledgedBy() {
        return acknowledgedBy;
    }

    public void setAcknowledgedBy(String acknowledgedBy) {
        this.acknowledgedBy = acknowledgedBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SupplyChainAlert that = (SupplyChainAlert) o;
        return Objects.equals(alertKey, that.alertKey);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(alertKey);
    }

    @Override
    public String toString() {
        return "SupplyChainAlert{" +
                "id=" + id +
                ", alertKey='" + alertKey + '\'' +
                ", alertType=" + alertType +
                ", alertStatus=" + alertStatus +
                ", entityType='" + entityType + '\'' +
                ", entityId=" + entityId +
                '}';
    }
}
