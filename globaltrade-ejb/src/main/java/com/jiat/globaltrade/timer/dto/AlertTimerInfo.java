package com.jiat.globaltrade.timer.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Serializable payload attached to programmatic EJB timers via TimerConfig.
 * Preserved in container timer store across server restarts when persistent=true.
 */
public class AlertTimerInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String alertType; // "SHIPMENT_ALERT" or "CUSTOMS_REMINDER"
    private final Long targetId;
    private final String referenceCode;
    private final String reason;
    private final LocalDateTime scheduledAt;
    private final long delayMillis;

    public AlertTimerInfo(String alertType, Long targetId, String referenceCode, String reason, long delayMillis) {
        this.alertType = alertType;
        this.targetId = targetId;
        this.referenceCode = referenceCode;
        this.reason = reason;
        this.scheduledAt = LocalDateTime.now();
        this.delayMillis = delayMillis;
    }

    public String getAlertType() {
        return alertType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public String getReferenceCode() {
        return referenceCode;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public long getDelayMillis() {
        return delayMillis;
    }

    @Override
    public String toString() {
        return "AlertTimerInfo{" +
                "alertType='" + alertType + '\'' +
                ", targetId=" + targetId +
                ", referenceCode='" + referenceCode + '\'' +
                ", reason='" + reason + '\'' +
                ", scheduledAt=" + scheduledAt +
                ", delayMillis=" + delayMillis +
                '}';
    }
}
