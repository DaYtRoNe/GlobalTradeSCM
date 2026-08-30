package com.jiat.globaltrade.service;

import com.jiat.globaltrade.entity.CustomsDocument;
import com.jiat.globaltrade.entity.Shipment;
import com.jiat.globaltrade.entity.enums.CustomsDocumentStatus;
import jakarta.ejb.EJB;
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
 * Core business service for customs declarations and regulatory compliance documentation.
 */
@Stateless
@TransactionManagement(TransactionManagementType.CONTAINER)
public class CustomsServiceBean {

    private static final Logger LOGGER = Logger.getLogger(CustomsServiceBean.class.getName());

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @EJB
    private AuditServiceBean auditService;

    /**
     * Read-only lookup of a customs document by ID.
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public CustomsDocument findCustomsDocumentById(Long id) {
        if (id == null) {
            return null;
        }
        return em.find(CustomsDocument.class, id);
    }

    /**
     * Read-only query for all documents associated with a shipment.
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<CustomsDocument> findDocumentsByShipment(Long shipmentId) {
        return em.createQuery("SELECT c FROM CustomsDocument c WHERE c.shipment.id = :shipmentId", CustomsDocument.class)
                .setParameter("shipmentId", shipmentId)
                .getResultList();
    }

    /**
     * Creates and persists a customs document linked to a specific shipment.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
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
        LOGGER.log(Level.INFO, "[CustomsServiceBean] [REQUIRED] Created customs document {0} for shipment {1}",
                new Object[]{document.getDocumentNumber(), shipment.getTrackingNumber()});

        auditService.logAction("CREATE_CUSTOMS_DOC", "CustomsDocument", document.getId(), performedBy,
                String.format("Doc: %s, Type: %s, Shipment: %s",
                        document.getDocumentNumber(), document.getDocumentType(), shipment.getTrackingNumber()));

        return document;
    }

    /**
     * Updates customs document clearance status.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
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

        LOGGER.log(Level.INFO, "[CustomsServiceBean] [REQUIRED] Customs doc {0} status updated from {1} to {2}",
                new Object[]{documentId, oldStatus, newStatus});

        auditService.logAction("UPDATE_CUSTOMS_STATUS", "CustomsDocument", documentId, performedBy,
                String.format("Status changed from %s to %s", oldStatus, newStatus));

        return doc;
    }
}
