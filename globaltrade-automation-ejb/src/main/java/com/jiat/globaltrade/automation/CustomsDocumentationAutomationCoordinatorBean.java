package com.jiat.globaltrade.automation;

import com.jiat.globaltrade.service.CustomsServiceBean;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Coordinator EJB for Automated Scheduled Customs Documentation Status Polling.
 * Runs in NOT_SUPPORTED transaction boundary. Caller identity propagates from entry-point bean.
 */
@Stateless
@TransactionManagement(TransactionManagementType.CONTAINER)
public class CustomsDocumentationAutomationCoordinatorBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(CustomsDocumentationAutomationCoordinatorBean.class.getName());

    @EJB
    private CustomsServiceBean customsService;

    @EJB
    private CustomsDocumentationAutomationWorkerBean workerBean;

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public CustomsPollingBatchSummary pollAllPendingDocuments() {
        LOGGER.log(Level.INFO, "[Automation-Coordinator] Starting scheduled customs clearance polling for pending documents...");

        List<Long> pendingDocIds = customsService.findPendingDocumentIds();

        int total = pendingDocIds != null ? pendingDocIds.size() : 0;
        int success = 0;
        int failed = 0;

        if (pendingDocIds != null) {
            for (Long docId : pendingDocIds) {
                try {
                    boolean result = workerBean.pollCustomsDocumentStatus(docId);
                    if (result) {
                        success++;
                    } else {
                        failed++;
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "[Automation-Coordinator] Error polling customs document #{0}: {1}",
                            new Object[]{docId, e.getMessage()});
                    failed++;
                }
            }
        }

        LOGGER.log(Level.INFO, "[Automation-Coordinator] Customs clearance polling complete: total={0}, success={1}, failed={2}",
                new Object[]{total, success, failed});

        return new CustomsPollingBatchSummary(total, success, failed);
    }

    public record CustomsPollingBatchSummary(int totalEvaluated, int successfulPolls, int failedPolls) implements Serializable {}
}
