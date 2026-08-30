package com.jiat.globaltrade.timer;

import com.jiat.globaltrade.entity.CustomsDocument;
import com.jiat.globaltrade.entity.Shipment;
import com.jiat.globaltrade.entity.enums.CustomsDocumentStatus;
import com.jiat.globaltrade.entity.enums.ShipmentStatus;
import com.jiat.globaltrade.service.AuditServiceBean;
import com.jiat.globaltrade.timer.dto.AlertTimerInfo;
import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.Timeout;
import jakarta.ejb.Timer;
import jakarta.ejb.TimerConfig;
import jakarta.ejb.TimerService;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * =================================================================================================
 * PROGRAMMATIC EJB TIMER SERVICE BEAN
 * -------------------------------------------------------------------------------------------------
 * Demonstrates dynamic, programmatic single-action EJB timers using TimerService, TimerConfig,
 * and @Timeout lifecycle callbacks.
 *
 * Use Case:
 * Schedulable single-action operational alerts for critical consignments or regulatory customs filings
 * (e.g. Expedite Shipment Check, Customs Submission Deadline Alert).
 *
 * Key Concepts Demonstrated:
 * 1. Programmatic Creation: timerService.createSingleActionTimer(delayMillis, new TimerConfig(info, true))
 * 2. Persistent Timers: persistent=true ensures single-action alert survives container/server restart
 * 3. Programmatic Cancellation: Searching container timers by attached Serializable info and canceling
 * 4. @Timeout Callback: Safe, transactional state inspection without mutating operational data
 * 5. Autonomous Audit Logging: AuditServiceBean (REQUIRES_NEW) records scheduling and expiration
 * =================================================================================================
 */
@Stateless
@TransactionManagement(TransactionManagementType.CONTAINER)
public class ShipmentAlertTimerBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(ShipmentAlertTimerBean.class.getName());

    @Resource
    private TimerService timerService;

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @EJB
    private AuditServiceBean auditService;

    /**
     * Programmatically schedules a single-action alert for a shipment after a specified millisecond delay.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public AlertTimerInfo scheduleShipmentAlert(Long shipmentId, long delayMillis, String reason) {
        if (shipmentId == null || delayMillis <= 0) {
            throw new IllegalArgumentException("Shipment ID must not be null and delay must be positive.");
        }

        Shipment shipment = em.find(Shipment.class, shipmentId);
        if (shipment == null) {
            throw new IllegalArgumentException("Shipment not found for ID: " + shipmentId);
        }

        // Cancel any existing pending timer for this shipment to avoid duplicates
        cancelShipmentAlert(shipmentId);

        String memo = reason != null && !reason.trim().isEmpty() ? reason : "Operational transit check";
        AlertTimerInfo timerInfo = new AlertTimerInfo("SHIPMENT_ALERT", shipmentId, shipment.getTrackingNumber(), memo, delayMillis);

        // persistent=true stores the programmatic timer in Payara's EJB timer repository
        TimerConfig timerConfig = new TimerConfig(timerInfo, true);
        Timer timer = timerService.createSingleActionTimer(delayMillis, timerConfig);

        LOGGER.log(Level.INFO, "[ShipmentAlertTimerBean] Programmatic single-action timer created for Shipment #{0} ({1}). Delay: {2} ms",
                new Object[]{shipmentId, shipment.getTrackingNumber(), delayMillis});

        auditService.logAction("PROGRAMMATIC_TIMER_SCHEDULED", "Shipment", shipmentId, "SYSTEM",
                String.format("Scheduled single-action alert '%s' to fire in %d ms (Tracking: %s)",
                        memo, delayMillis, shipment.getTrackingNumber()));

        return timerInfo;
    }

    /**
     * Programmatically schedules a single-action reminder for a customs document after a specified delay.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public AlertTimerInfo scheduleCustomsReminder(Long documentId, long delayMillis, String reason) {
        if (documentId == null || delayMillis <= 0) {
            throw new IllegalArgumentException("Document ID must not be null and delay must be positive.");
        }

        CustomsDocument doc = em.find(CustomsDocument.class, documentId);
        if (doc == null) {
            throw new IllegalArgumentException("CustomsDocument not found for ID: " + documentId);
        }

        // Cancel existing pending timer for this document
        cancelCustomsReminder(documentId);

        String memo = reason != null && !reason.trim().isEmpty() ? reason : "Filing deadline reminder";
        AlertTimerInfo timerInfo = new AlertTimerInfo("CUSTOMS_REMINDER", documentId, doc.getDocumentNumber(), memo, delayMillis);

        TimerConfig timerConfig = new TimerConfig(timerInfo, true);
        Timer timer = timerService.createSingleActionTimer(delayMillis, timerConfig);

        LOGGER.log(Level.INFO, "[ShipmentAlertTimerBean] Programmatic single-action timer created for CustomsDocument #{0} ({1}). Delay: {2} ms",
                new Object[]{documentId, doc.getDocumentNumber(), delayMillis});

        auditService.logAction("PROGRAMMATIC_TIMER_SCHEDULED", "CustomsDocument", documentId, "SYSTEM",
                String.format("Scheduled customs filing reminder '%s' to fire in %d ms (Doc: %s)",
                        memo, delayMillis, doc.getDocumentNumber()));

        return timerInfo;
    }

    /**
     * Programmatically cancels an active single-action shipment alert timer.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public boolean cancelShipmentAlert(Long shipmentId) {
        if (shipmentId == null) {
            return false;
        }

        Collection<Timer> timers = timerService.getTimers();
        for (Timer timer : timers) {
            Serializable info = timer.getInfo();
            if (info instanceof AlertTimerInfo alertInfo) {
                if ("SHIPMENT_ALERT".equals(alertInfo.getAlertType()) && shipmentId.equals(alertInfo.getTargetId())) {
                    timer.cancel();
                    LOGGER.log(Level.INFO, "[ShipmentAlertTimerBean] Programmatic timer cancelled for Shipment #{0}", shipmentId);

                    auditService.logAction("PROGRAMMATIC_TIMER_CANCELLED", "Shipment", shipmentId, "SYSTEM",
                            String.format("Cancelled programmatic alert for tracking %s", alertInfo.getReferenceCode()));
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Programmatically cancels an active customs reminder timer.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public boolean cancelCustomsReminder(Long documentId) {
        if (documentId == null) {
            return false;
        }

        Collection<Timer> timers = timerService.getTimers();
        for (Timer timer : timers) {
            Serializable info = timer.getInfo();
            if (info instanceof AlertTimerInfo alertInfo) {
                if ("CUSTOMS_REMINDER".equals(alertInfo.getAlertType()) && documentId.equals(alertInfo.getTargetId())) {
                    timer.cancel();
                    LOGGER.log(Level.INFO, "[ShipmentAlertTimerBean] Programmatic timer cancelled for CustomsDocument #{0}", documentId);

                    auditService.logAction("PROGRAMMATIC_TIMER_CANCELLED", "CustomsDocument", documentId, "SYSTEM",
                            String.format("Cancelled programmatic reminder for doc %s", alertInfo.getReferenceCode()));
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns a summary list of all currently active programmatic timers in this bean.
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<AlertTimerInfo> getActiveTimers() {
        List<AlertTimerInfo> active = new ArrayList<>();
        Collection<Timer> timers = timerService.getTimers();
        for (Timer timer : timers) {
            Serializable info = timer.getInfo();
            if (info instanceof AlertTimerInfo alertInfo) {
                active.add(alertInfo);
            }
        }
        return active;
    }

    /**
     * Programmatic Timer Expiration Callback.
     * Container invokes this method automatically when a single-action timer expires.
     */
    @Timeout
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void onTimeout(Timer timer) {
        Serializable info = timer.getInfo();
        LOGGER.log(Level.INFO, "[ShipmentAlertTimerBean] @Timeout callback received for timer: {0}", info);

        if (!(info instanceof AlertTimerInfo alertInfo)) {
            LOGGER.log(Level.WARNING, "[ShipmentAlertTimerBean] Unrecognized timer info payload: {0}", info);
            return;
        }

        if ("SHIPMENT_ALERT".equals(alertInfo.getAlertType())) {
            handleShipmentAlertTimeout(alertInfo);
        } else if ("CUSTOMS_REMINDER".equals(alertInfo.getAlertType())) {
            handleCustomsReminderTimeout(alertInfo);
        }
    }

    private void handleShipmentAlertTimeout(AlertTimerInfo alertInfo) {
        Shipment shipment = em.find(Shipment.class, alertInfo.getTargetId());
        if (shipment == null) {
            LOGGER.log(Level.WARNING, "[ShipmentAlertTimerBean] Timeout triggered for non-existent Shipment #{0}", alertInfo.getTargetId());
            return;
        }

        if (shipment.getShipmentStatus() == ShipmentStatus.DELIVERED) {
            LOGGER.log(Level.INFO, "[ShipmentAlertTimerBean] Shipment #{0} ({1}) is already DELIVERED. Alert resolved with no action required.",
                    new Object[]{shipment.getId(), shipment.getTrackingNumber()});

            auditService.logAction("PROGRAMMATIC_TIMER_RESOLVED", "Shipment", shipment.getId(), "SYSTEM",
                    String.format("Alert '%s' expired. Shipment %s is already delivered.",
                            alertInfo.getReason(), shipment.getTrackingNumber()));
        } else {
            LOGGER.log(Level.WARNING, "[ShipmentAlertTimerBean] Shipment #{0} ({1}) alert triggered! Status: {2}, Expected: {3}",
                    new Object[]{shipment.getId(), shipment.getTrackingNumber(), shipment.getShipmentStatus(), shipment.getExpectedDeliveryDate()});

            auditService.logAction("PROGRAMMATIC_TIMER_FIRED", "Shipment", shipment.getId(), "SYSTEM",
                    String.format("Expedite alert triggered: '%s'. Tracking: %s, Current status: %s",
                            alertInfo.getReason(), shipment.getTrackingNumber(), shipment.getShipmentStatus()));
        }
    }

    private void handleCustomsReminderTimeout(AlertTimerInfo alertInfo) {
        CustomsDocument doc = em.find(CustomsDocument.class, alertInfo.getTargetId());
        if (doc == null) {
            LOGGER.log(Level.WARNING, "[ShipmentAlertTimerBean] Timeout triggered for non-existent CustomsDocument #{0}", alertInfo.getTargetId());
            return;
        }

        if (doc.getStatus() == CustomsDocumentStatus.APPROVED) {
            LOGGER.log(Level.INFO, "[ShipmentAlertTimerBean] Customs document #{0} ({1}) is already APPROVED. Reminder resolved.",
                    new Object[]{doc.getId(), doc.getDocumentNumber()});

            auditService.logAction("PROGRAMMATIC_TIMER_RESOLVED", "CustomsDocument", doc.getId(), "SYSTEM",
                    String.format("Reminder '%s' expired. Document %s is already approved.",
                            alertInfo.getReason(), doc.getDocumentNumber()));
        } else {
            LOGGER.log(Level.WARNING, "[ShipmentAlertTimerBean] Customs reminder triggered for doc #{0} ({1})! Status: {2}, Deadline: {3}",
                    new Object[]{doc.getId(), doc.getDocumentNumber(), doc.getStatus(), doc.getSubmissionDeadline()});

            auditService.logAction("PROGRAMMATIC_TIMER_FIRED", "CustomsDocument", doc.getId(), "SYSTEM",
                    String.format("Customs filing reminder: '%s'. Doc: %s, Status: %s, Deadline: %s",
                            alertInfo.getReason(), doc.getDocumentNumber(), doc.getStatus(), doc.getSubmissionDeadline()));
        }
    }
}
