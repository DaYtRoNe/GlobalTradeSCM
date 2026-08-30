package com.jiat.globaltrade.service;

import com.jiat.globaltrade.entity.AuditLog;
import com.jiat.globaltrade.security.SecurityRoles;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.PermitAll;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Stateless EJB responsible for persisting and querying audit log records.
 * Uses REQUIRES_NEW for write operations so that audit records are committed
 * independently of any caller business transaction, ensuring audit trails
 * are preserved even if the business transaction rolls back.
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
public class AuditServiceBean {

    private static final Logger LOGGER = Logger.getLogger(AuditServiceBean.class.getName());

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    /**
     * Persists an audit log record in an independent transaction.
     * Accessible by all components, interceptors, and timer beans.
     */
    @PermitAll
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public AuditLog logAction(String action, String entityType, Long entityId, String performedBy, String details) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setAction(action);
            auditLog.setEntityType(entityType);
            auditLog.setEntityId(entityId);
            auditLog.setPerformedBy(performedBy != null ? performedBy : "SYSTEM");
            auditLog.setTimestamp(LocalDateTime.now());
            auditLog.setDetails(details);

            em.persist(auditLog);
            LOGGER.log(Level.INFO, "[AuditServiceBean] [REQUIRES_NEW] Audit log recorded: action={0}, entity={1}#{2}, user={3}",
                    new Object[]{action, entityType, entityId, performedBy});
            return auditLog;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[AuditServiceBean] Failed to write audit log: " + e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Read-only retrieval of the most recent audit logs.
     * SUPPORTS allows participation in an existing transaction if present, or executes without one.
     */
    @PermitAll
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<AuditLog> getRecentLogs(int limit) {
        int max = limit > 0 ? limit : 50;
        return em.createQuery("SELECT a FROM AuditLog a ORDER BY a.timestamp DESC", AuditLog.class)
                .setMaxResults(max)
                .getResultList();
    }

    /**
     * Returns the total count of audit logs recorded.
     */
    @PermitAll
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public long getAuditLogCount() {
        return em.createQuery("SELECT COUNT(a) FROM AuditLog a", Long.class)
                .getSingleResult();
    }
}
