package com.jiat.globaltrade.automation;

import com.jiat.globaltrade.entity.CustomsDocument;
import com.jiat.globaltrade.entity.enums.CustomsDocumentStatus;
import com.jiat.globaltrade.integration.gateway.CustomsSystemGateway;
import com.jiat.globaltrade.integration.model.CustomsEdiPayload;
import com.jiat.globaltrade.service.AuditServiceBean;
import com.jiat.globaltrade.service.CustomsServiceBean;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Worker EJB for Automated Scheduled Customs Documentation Polling.
 * Interrogates external national EDI platform in REQUIRES_NEW without mutating statutory approval state.
 * Caller identity (SYSTEM from timer, ADMIN from diagnostic/test) propagates from the entry-point bean.
 * Uses core CustomsServiceBean and AuditServiceBean via EJB injection.
 */
@Stateless
@TransactionManagement(TransactionManagementType.CONTAINER)
public class CustomsDocumentationAutomationWorkerBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(CustomsDocumentationAutomationWorkerBean.class.getName());

    @EJB
    private CustomsServiceBean customsService;

    @EJB
    private CustomsSystemGateway customsGateway;

    @EJB
    private AuditServiceBean auditService;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean pollCustomsDocumentStatus(Long documentId) {
        if (documentId == null) {
            return false;
        }

        CustomsDocument doc = customsService.findCustomsDocumentById(documentId);
        if (doc == null || doc.getStatus() == CustomsDocumentStatus.APPROVED
                || doc.getStatus() == CustomsDocumentStatus.REJECTED) {
            return false;
        }

        CustomsEdiPayload edi = customsGateway.queryClearanceStatus(doc.getDocumentNumber());
        if (edi != null) {
            LOGGER.log(Level.INFO, "[Automation-Worker] Polled customs EDI for doc {0}: Authority={1}, Status={2}",
                    new Object[]{doc.getDocumentNumber(), edi.customsAuthority(), edi.clearanceStatusCode()});

            auditService.logAction(
                    "CUSTOMS_DOCUMENT_STATUS_POLLED",
                    "CustomsDocument",
                    doc.getId(),
                    "SYSTEM_AUTOMATION",
                    String.format("Polled national EDI clearance authority (%s) for %s: Status=%s, EntryNum=%s (Mode: %s)",
                            edi.customsAuthority(), doc.getDocumentNumber(), edi.clearanceStatusCode(), edi.entryNumber(), edi.integrationMode())
            );
            return true;
        }
        return false;
    }
}
