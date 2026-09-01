package com.jiat.globaltrade.service;

import com.jiat.globaltrade.entity.Shipment;
import com.jiat.globaltrade.entity.SupplyChainAlert;
import com.jiat.globaltrade.entity.enums.SupplyChainAlertStatus;
import com.jiat.globaltrade.entity.enums.SupplyChainAlertType;
import com.jiat.globaltrade.exception.ResourceNotFoundException;
import com.jiat.globaltrade.security.SecurityRoles;
import jakarta.annotation.Resource;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBAccessException;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Core business service managing persistent Supply Chain Alerts.
 * Handles idempotent alert detection, automatic condition resolution, role-authorized
 * acknowledgements, and server-side role-based alert visibility.
 */
@Stateless
@TransactionManagement(TransactionManagementType.CONTAINER)
@DeclareRoles({
        SecurityRoles.ADMIN,
        SecurityRoles.LOGISTICS_COORDINATOR,
        SecurityRoles.CUSTOMS_AGENT,
        SecurityRoles.WAREHOUSE_MANAGER,
        SecurityRoles.VENDOR_REPRESENTATIVE,
        SecurityRoles.CUSTOMER,
        SecurityRoles.SYSTEM
})
public class SupplyChainAlertServiceBean {

    private static final Logger LOGGER = Logger.getLogger(SupplyChainAlertServiceBean.class.getName());

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @Resource
    private SessionContext sessionContext;

    @EJB
    private AuditServiceBean auditService;

    /**
     * Idempotently processes an active anomaly condition.
     * - Creates a new OPEN alert if none exists.
     * - Updates lastDetectedAt if OPEN or ACKNOWLEDGED (avoiding duplicate audit logs).
     * - Reopens the alert if previously RESOLVED.
     */
    @PermitAll
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public SupplyChainAlert processActiveCondition(
            String alertKey,
            SupplyChainAlertType alertType,
            String entityType,
            Long entityId,
            String message,
            String performedBy) {

        if (alertKey == null || alertType == null || entityType == null || entityId == null) {
            throw new IllegalArgumentException("Alert key, type, entity type, and entity ID must not be null.");
        }

        SupplyChainAlert alert = findByAlertKey(alertKey);

        if (alert == null) {
            alert = new SupplyChainAlert(alertKey, alertType, entityType, entityId, message);
            em.persist(alert);
            em.flush();

            LOGGER.log(Level.WARNING, "[SupplyChainAlertServiceBean] Alert OPENED: {0} ({1})",
                    new Object[]{alertKey, message});

            auditService.logAction("ALERT_OPENED", entityType, entityId, performedBy,
                    String.format("[%s] %s", alertType, message));

            return alert;
        }

        if (alert.getAlertStatus() == SupplyChainAlertStatus.RESOLVED) {
            alert.setAlertStatus(SupplyChainAlertStatus.OPEN);
            alert.setLastDetectedAt(LocalDateTime.now());
            alert.setResolvedAt(null);
            alert.setAcknowledgedAt(null);
            alert.setAcknowledgedBy(null);
            alert.setMessage(message);
            em.merge(alert);
            em.flush();

            LOGGER.log(Level.WARNING, "[SupplyChainAlertServiceBean] Alert REOPENED: {0} ({1})",
                    new Object[]{alertKey, message});

            auditService.logAction("ALERT_REOPENED", entityType, entityId, performedBy,
                    String.format("[%s] %s", alertType, message));

            return alert;
        }

        // Alert is OPEN or ACKNOWLEDGED: update heartbeat timestamp without spamming audit log
        alert.setLastDetectedAt(LocalDateTime.now());
        alert.setMessage(message);
        em.merge(alert);
        return alert;
    }

    /**
     * Resolves an alert when its monitored condition has cleared.
     */
    @PermitAll
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public boolean processClearedCondition(String alertKey, String performedBy) {
        if (alertKey == null) {
            return false;
        }

        SupplyChainAlert alert = findByAlertKey(alertKey);
        if (alert != null && alert.getAlertStatus() != SupplyChainAlertStatus.RESOLVED) {
            alert.setAlertStatus(SupplyChainAlertStatus.RESOLVED);
            alert.setResolvedAt(LocalDateTime.now());
            em.merge(alert);
            em.flush();

            LOGGER.log(Level.INFO, "[SupplyChainAlertServiceBean] Alert RESOLVED: {0}", alertKey);

            auditService.logAction("ALERT_RESOLVED", alert.getEntityType(), alert.getEntityId(), performedBy,
                    String.format("[%s] Problem condition cleared for %s", alert.getAlertType(), alertKey));

            return true;
        }

        return false;
    }

    /**
     * Acknowledges an active alert.
     * Enforces fine-grained caller authorization.
     */
    @RolesAllowed({
            SecurityRoles.ADMIN,
            SecurityRoles.LOGISTICS_COORDINATOR,
            SecurityRoles.CUSTOMS_AGENT,
            SecurityRoles.WAREHOUSE_MANAGER,
            SecurityRoles.VENDOR_REPRESENTATIVE,
            SecurityRoles.CUSTOMER
    })
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public SupplyChainAlert acknowledgeAlert(Long alertId) throws ResourceNotFoundException {
        if (alertId == null) {
            throw new IllegalArgumentException("Alert ID must not be null.");
        }

        SupplyChainAlert alert = em.find(SupplyChainAlert.class, alertId);
        if (alert == null) {
            throw new ResourceNotFoundException("SupplyChainAlert", alertId);
        }

        if (!isAlertVisibleToCaller(alert)) {
            String caller = sessionContext.getCallerPrincipal() != null ? sessionContext.getCallerPrincipal().getName() : "ANONYMOUS";
            LOGGER.log(Level.WARNING, "[SupplyChainAlertServiceBean] Caller {0} denied authorization to acknowledge alert #{1}",
                    new Object[]{caller, alertId});
            throw new EJBAccessException("Caller is not authorized to acknowledge this alert.");
        }

        if (alert.getAlertStatus() == SupplyChainAlertStatus.OPEN) {
            String caller = sessionContext.getCallerPrincipal() != null ? sessionContext.getCallerPrincipal().getName() : "ANONYMOUS";
            alert.setAlertStatus(SupplyChainAlertStatus.ACKNOWLEDGED);
            alert.setAcknowledgedAt(LocalDateTime.now());
            alert.setAcknowledgedBy(caller);
            em.merge(alert);
            em.flush();

            LOGGER.log(Level.INFO, "[SupplyChainAlertServiceBean] Alert #{0} ACKNOWLEDGED by {1}",
                    new Object[]{alertId, caller});

            auditService.logAction("ALERT_ACKNOWLEDGED", alert.getEntityType(), alert.getEntityId(), caller,
                    String.format("Alert %s acknowledged by %s", alert.getAlertKey(), caller));
        }

        return alert;
    }

    /**
     * Queries all alerts visible to the authenticated caller based on JAAS roles.
     */
    @RolesAllowed({
            SecurityRoles.ADMIN,
            SecurityRoles.LOGISTICS_COORDINATOR,
            SecurityRoles.CUSTOMS_AGENT,
            SecurityRoles.WAREHOUSE_MANAGER,
            SecurityRoles.VENDOR_REPRESENTATIVE,
            SecurityRoles.CUSTOMER
    })
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<SupplyChainAlert> findAlertsForCaller(SupplyChainAlertStatus filterStatus) {
        String caller = sessionContext.getCallerPrincipal() != null ? sessionContext.getCallerPrincipal().getName() : "ANONYMOUS";

        if (sessionContext.isCallerInRole(SecurityRoles.ADMIN) || sessionContext.isCallerInRole(SecurityRoles.LOGISTICS_COORDINATOR)) {
            // ADMIN & LOGISTICS_COORDINATOR: Global operational access
            return queryAlerts(null, null, null, filterStatus);
        }

        if (sessionContext.isCallerInRole(SecurityRoles.WAREHOUSE_MANAGER)) {
            // WAREHOUSE_MANAGER: Inventory shortages and shipment delay alerts
            List<SupplyChainAlertType> types = List.of(
                    SupplyChainAlertType.INVENTORY_REPLENISHMENT_REQUIRED,
                    SupplyChainAlertType.SHIPMENT_DELAY
            );
            return queryAlerts(types, null, null, filterStatus);
        }

        if (sessionContext.isCallerInRole(SecurityRoles.CUSTOMS_AGENT)) {
            // CUSTOMS_AGENT: Customs deadlines and shipment delay alerts
            List<SupplyChainAlertType> types = List.of(
                    SupplyChainAlertType.CUSTOMS_DOCUMENT_DEADLINE,
                    SupplyChainAlertType.SHIPMENT_DELAY
            );
            return queryAlerts(types, null, null, filterStatus);
        }

        if (sessionContext.isCallerInRole(SecurityRoles.VENDOR_REPRESENTATIVE)) {
            // VENDOR_REPRESENTATIVE: Only performance alerts for own mapped vendor
            Long vendorId = findVendorIdForCaller(caller);
            if (vendorId == null) {
                return Collections.emptyList();
            }
            return queryAlerts(
                    List.of(SupplyChainAlertType.VENDOR_PERFORMANCE_RISK),
                    "Vendor",
                    vendorId,
                    filterStatus
            );
        }

        if (sessionContext.isCallerInRole(SecurityRoles.CUSTOMER)) {
            // CUSTOMER: Only shipment delay alerts for own shipments
            List<Long> customerShipmentIds = em.createQuery(
                    "SELECT s.id FROM Shipment s WHERE s.customerUsername = :username", Long.class)
                    .setParameter("username", caller)
                    .getResultList();

            if (customerShipmentIds.isEmpty()) {
                return Collections.emptyList();
            }

            StringBuilder jpql = new StringBuilder(
                    "SELECT a FROM SupplyChainAlert a WHERE a.alertType = :type AND a.entityType = 'Shipment' AND a.entityId IN :shipmentIds");
            if (filterStatus != null) {
                jpql.append(" AND a.alertStatus = :status");
            }
            jpql.append(" ORDER BY a.detectedAt DESC");

            TypedQuery<SupplyChainAlert> query = em.createQuery(jpql.toString(), SupplyChainAlert.class)
                    .setParameter("type", SupplyChainAlertType.SHIPMENT_DELAY)
                    .setParameter("shipmentIds", customerShipmentIds);

            if (filterStatus != null) {
                query.setParameter("status", filterStatus);
            }

            return query.getResultList();
        }

        return Collections.emptyList();
    }

    private List<SupplyChainAlert> queryAlerts(
            List<SupplyChainAlertType> alertTypes,
            String entityType,
            Long entityId,
            SupplyChainAlertStatus filterStatus) {

        StringBuilder jpql = new StringBuilder("SELECT a FROM SupplyChainAlert a WHERE 1=1");

        if (alertTypes != null && !alertTypes.isEmpty()) {
            jpql.append(" AND a.alertType IN :types");
        }
        if (entityType != null) {
            jpql.append(" AND a.entityType = :entityType");
        }
        if (entityId != null) {
            jpql.append(" AND a.entityId = :entityId");
        }
        if (filterStatus != null) {
            jpql.append(" AND a.alertStatus = :status");
        }

        jpql.append(" ORDER BY a.detectedAt DESC");

        TypedQuery<SupplyChainAlert> query = em.createQuery(jpql.toString(), SupplyChainAlert.class);

        if (alertTypes != null && !alertTypes.isEmpty()) {
            query.setParameter("types", alertTypes);
        }
        if (entityType != null) {
            query.setParameter("entityType", entityType);
        }
        if (entityId != null) {
            query.setParameter("entityId", entityId);
        }
        if (filterStatus != null) {
            query.setParameter("status", filterStatus);
        }

        return query.getResultList();
    }

    private boolean isAlertVisibleToCaller(SupplyChainAlert alert) {
        String caller = sessionContext.getCallerPrincipal() != null ? sessionContext.getCallerPrincipal().getName() : "ANONYMOUS";

        if (sessionContext.isCallerInRole(SecurityRoles.ADMIN) || sessionContext.isCallerInRole(SecurityRoles.LOGISTICS_COORDINATOR)) {
            return true;
        }

        if (sessionContext.isCallerInRole(SecurityRoles.WAREHOUSE_MANAGER)) {
            return alert.getAlertType() == SupplyChainAlertType.INVENTORY_REPLENISHMENT_REQUIRED
                    || alert.getAlertType() == SupplyChainAlertType.SHIPMENT_DELAY;
        }

        if (sessionContext.isCallerInRole(SecurityRoles.CUSTOMS_AGENT)) {
            return alert.getAlertType() == SupplyChainAlertType.CUSTOMS_DOCUMENT_DEADLINE
                    || alert.getAlertType() == SupplyChainAlertType.SHIPMENT_DELAY;
        }

        if (sessionContext.isCallerInRole(SecurityRoles.VENDOR_REPRESENTATIVE)) {
            if (alert.getAlertType() != SupplyChainAlertType.VENDOR_PERFORMANCE_RISK) {
                return false;
            }
            Long vendorId = findVendorIdForCaller(caller);
            return vendorId != null && vendorId.equals(alert.getEntityId());
        }

        if (sessionContext.isCallerInRole(SecurityRoles.CUSTOMER)) {
            if (alert.getAlertType() != SupplyChainAlertType.SHIPMENT_DELAY || !"Shipment".equals(alert.getEntityType())) {
                return false;
            }
            Shipment s = em.find(Shipment.class, alert.getEntityId());
            return s != null && caller.equals(s.getCustomerUsername());
        }

        return false;
    }

    private Long findVendorIdForCaller(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        try {
            List<?> list = em.createNativeQuery(
                    "SELECT vendor_id FROM vendor_user_access WHERE username = ?1")
                    .setParameter(1, username)
                    .getResultList();
            if (list != null && !list.isEmpty()) {
                Object obj = list.get(0);
                if (obj instanceof Number n) {
                    return n.longValue();
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[SupplyChainAlertServiceBean] Error querying vendor_user_access for caller: " + username, e);
        }
        return null;
    }

    @PermitAll
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public SupplyChainAlert findByAlertKey(String alertKey) {
        List<SupplyChainAlert> list = em.createQuery(
                "SELECT a FROM SupplyChainAlert a WHERE a.alertKey = :key", SupplyChainAlert.class)
                .setParameter("key", alertKey)
                .getResultList();
        return list.isEmpty() ? null : list.get(0);
    }
}
