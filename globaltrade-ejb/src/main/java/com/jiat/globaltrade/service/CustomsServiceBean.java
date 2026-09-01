package com.jiat.globaltrade.service;

import com.jiat.globaltrade.entity.CustomsDocument;
import com.jiat.globaltrade.entity.Shipment;
import com.jiat.globaltrade.entity.enums.CustomsDocumentStatus;
import com.jiat.globaltrade.exception.ResourceNotFoundException;
import com.jiat.globaltrade.exception.ShipmentAccessDeniedException;
import com.jiat.globaltrade.interceptor.BusinessAuditInterceptor;
import com.jiat.globaltrade.interceptor.BusinessValidationInterceptor;
import com.jiat.globaltrade.interceptor.PerformanceMonitoringInterceptor;
import com.jiat.globaltrade.interceptor.TradeComplianceInterceptor;
import com.jiat.globaltrade.security.SecurityRoles;
import jakarta.annotation.Resource;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Core business service for customs declarations and regulatory compliance documentation.
 * Demonstrates Class-Level & Method-Level Interceptor Combination and Declarative RBAC.
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
public class CustomsServiceBean {

    private static final Logger LOGGER = Logger.getLogger(CustomsServiceBean.class.getName());

    @Resource
    private SessionContext sessionContext;

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @EJB
    private AuditServiceBean auditService;

    /**
     * Read-only lookup of a customs document by ID.
     */
    @PermitAll
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public CustomsDocument findCustomsDocumentById(Long id) {
        if (id == null) {
            return null;
        }
        return em.find(CustomsDocument.class, id);
    }

    /**
     * Read-only global list of all customs documents for authorized staff.
     */
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.CUSTOMS_AGENT, SecurityRoles.LOGISTICS_COORDINATOR})
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<CustomsDocument> findAllCustomsDocuments() {
        return em.createQuery("SELECT c FROM CustomsDocument c ORDER BY c.createdAt DESC", CustomsDocument.class)
                .getResultList();
    }

    /**
     * Read-only lookup of all pending customs document IDs (not approved and not rejected).
     */
    @PermitAll
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Long> findPendingDocumentIds() {
        return em.createQuery(
                "SELECT d.id FROM CustomsDocument d WHERE d.status <> :approved AND d.status <> :rejected ORDER BY d.id ASC", Long.class)
                .setParameter("approved", CustomsDocumentStatus.APPROVED)
                .setParameter("rejected", CustomsDocumentStatus.REJECTED)
                .getResultList();
    }

    /**
     * Read-only query for all documents associated with a shipment.
     */
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.CUSTOMS_AGENT, SecurityRoles.LOGISTICS_COORDINATOR})
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<CustomsDocument> findDocumentsByShipment(Long shipmentId) {
        return em.createQuery("SELECT c FROM CustomsDocument c WHERE c.shipment.id = :shipmentId ORDER BY c.createdAt ASC", CustomsDocument.class)
                .setParameter("shipmentId", shipmentId)
                .getResultList();
    }

    /**
     * Secure customs document query enforcing customer shipment ownership.
     * - Staff roles: allowed to view customs documents for any shipment.
     * - CUSTOMER role: allowed ONLY if the parent shipment belongs to the authenticated caller.
     */
    @RolesAllowed({
            SecurityRoles.ADMIN,
            SecurityRoles.CUSTOMS_AGENT,
            SecurityRoles.LOGISTICS_COORDINATOR,
            SecurityRoles.CUSTOMER
    })
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<CustomsDocument> findDocumentsByShipmentForCaller(Long shipmentId)
            throws ResourceNotFoundException, ShipmentAccessDeniedException {
        if (shipmentId == null) {
            throw new ResourceNotFoundException("Shipment", shipmentId);
        }

        Shipment shipment = em.find(Shipment.class, shipmentId);
        if (shipment == null) {
            throw new ResourceNotFoundException("Shipment", shipmentId);
        }

        if (sessionContext.isCallerInRole(SecurityRoles.CUSTOMER)) {
            String callerUsername = sessionContext.getCallerPrincipal() != null ? sessionContext.getCallerPrincipal().getName() : "ANONYMOUS";
            if (shipment.getCustomerUsername() == null || !shipment.getCustomerUsername().equals(callerUsername)) {
                LOGGER.log(Level.WARNING, "[CustomsServiceBean] Customer {0} denied access to customs for unowned Shipment #{1}",
                        new Object[]{callerUsername, shipmentId});
                throw new ShipmentAccessDeniedException(shipmentId, callerUsername);
            }
        }

        return findDocumentsByShipment(shipmentId);
    }

    /**
     * Creates and persists a customs document linked to a specific shipment.
     * Demonstrates Intentional Method-Level Interceptor Chaining Order:
     * 1. BusinessValidationInterceptor (Input parameter validation)
     * 2. TradeComplianceInterceptor (Regulatory format and compliance checks)
     * 3. PerformanceMonitoringInterceptor (Execution timing measurement)
     * 4. BusinessAuditInterceptor (Invocation auditing)
     */
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.CUSTOMS_AGENT})
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @Interceptors({
            BusinessValidationInterceptor.class,
            TradeComplianceInterceptor.class,
            PerformanceMonitoringInterceptor.class,
            BusinessAuditInterceptor.class
    })
    public CustomsDocument createCustomsDocument(CustomsDocument document, Long shipmentId, String performedBy) {
        if (document == null || shipmentId == null) {
            throw new IllegalArgumentException("Document and shipment ID must not be null.");
        }

        Shipment shipment = em.find(Shipment.class, shipmentId);
        if (shipment == null) {
            throw new IllegalArgumentException("Shipment not found for ID: " + shipmentId);
        }

        document.setShipment(shipment);
        if (document.getCreatedAt() == null) {
            document.setCreatedAt(LocalDateTime.now());
        }
        if (document.getStatus() == null) {
            document.setStatus(CustomsDocumentStatus.PENDING);
        }

        em.persist(document);
        em.flush(); // Immediately synchronizes with DB and populates generated ID (IDENTITY)

        LOGGER.log(Level.INFO, "[CustomsServiceBean] [REQUIRED] Created customs document #{0} ({1}) for shipment {2}",
                new Object[]{document.getId(), document.getDocumentNumber(), shipment.getTrackingNumber()});

        auditService.logAction("CREATE_CUSTOMS_DOC", "CustomsDocument", document.getId(), performedBy,
                String.format("Doc: %s, Type: %s, Shipment: %s",
                        document.getDocumentNumber(), document.getDocumentType(), shipment.getTrackingNumber()));

        return document;
    }

    /**
     * Updates customs document clearance status.
     * Demonstrates Intentional Method-Level Interceptor Chaining Order:
     * 1. BusinessValidationInterceptor
     * 2. TradeComplianceInterceptor
     * 3. PerformanceMonitoringInterceptor
     * 4. BusinessAuditInterceptor
     */
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.CUSTOMS_AGENT})
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @Interceptors({
            BusinessValidationInterceptor.class,
            TradeComplianceInterceptor.class,
            PerformanceMonitoringInterceptor.class,
            BusinessAuditInterceptor.class
    })
    public CustomsDocument updateDocumentStatus(Long documentId, CustomsDocumentStatus newStatus, String performedBy) {
        if (documentId == null || newStatus == null) {
            throw new IllegalArgumentException("Document ID and status must not be null.");
        }

        CustomsDocument doc = em.find(CustomsDocument.class, documentId);
        if (doc == null) {
            LOGGER.log(Level.WARNING, "[CustomsServiceBean] CustomsDocument not found for ID: {0}", documentId);
            return null;
        }

        CustomsDocumentStatus oldStatus = doc.getStatus();
        doc.setStatus(newStatus);
        em.merge(doc);

        LOGGER.log(Level.INFO, "[CustomsServiceBean] [REQUIRED] Customs doc #{0} ({1}) status updated from {2} to {3}",
                new Object[]{documentId, doc.getDocumentNumber(), oldStatus, newStatus});

        auditService.logAction("UPDATE_CUSTOMS_STATUS", "CustomsDocument", documentId, performedBy,
                String.format("Status changed from %s to %s", oldStatus, newStatus));

        return doc;
    }
}
