package com.jiat.globaltrade.automation.test;

import com.jiat.globaltrade.automation.AutomationDiagnosticServiceBean;
import com.jiat.globaltrade.entity.CustomsDocument;
import com.jiat.globaltrade.entity.enums.CustomsDocumentStatus;
import com.jiat.globaltrade.entity.enums.CustomsDocumentType;
import com.jiat.globaltrade.security.SecurityRoles;
import com.jiat.globaltrade.service.CustomsServiceBean;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RunAs;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;

import com.jiat.globaltrade.automation.ShipmentTrackingAutomationWorkerBean;
import com.jiat.globaltrade.automation.ShipmentTrackingAutomationCoordinatorBean;
import com.jiat.globaltrade.automation.InventoryReplenishmentAutomationWorkerBean;
import com.jiat.globaltrade.automation.InventoryReplenishmentAutomationCoordinatorBean;
import com.jiat.globaltrade.automation.CustomsDocumentationAutomationWorkerBean;

/**
 * Test-only invoker EJB executing under the ADMIN security identity.
 * Follows the exact proven pattern established by AdminTestInvoker in globaltrade-ejb.
 *
 * <ul>
 *   <li>{@code @Stateless} - pooled EJB instance</li>
 *   <li>{@code @PermitAll} - allows the unauthenticated Arquillian test thread to enter this bean</li>
 *   <li>{@code @RunAs(ADMIN)} - propagates ADMIN identity to all downstream EJB calls</li>
 *   <li>{@code @DeclareRoles} - declares all roles this bean's outgoing call chain may touch</li>
 * </ul>
 */
@Stateless
@PermitAll
@RunAs(SecurityRoles.ADMIN)
@DeclareRoles({
        SecurityRoles.ADMIN,
        SecurityRoles.CUSTOMS_AGENT,
        SecurityRoles.LOGISTICS_COORDINATOR,
        SecurityRoles.WAREHOUSE_MANAGER,
        SecurityRoles.SYSTEM
})
public class AutomationTestInvoker {

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @EJB
    private CustomsServiceBean customsService;

    @EJB
    private AutomationDiagnosticServiceBean automationDiagnosticService;

    @EJB
    private ShipmentTrackingAutomationWorkerBean trackingWorker;

    @EJB
    private ShipmentTrackingAutomationCoordinatorBean trackingCoordinator;

    @EJB
    private InventoryReplenishmentAutomationWorkerBean replenishmentWorker;

    @EJB
    private InventoryReplenishmentAutomationCoordinatorBean replenishmentCoordinator;

    @EJB
    private CustomsDocumentationAutomationWorkerBean customsWorker;

    // ── Admin fixture operations ──

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Long createOrUpdateCustomsDocument(String docNum, Long shipmentId, CustomsDocumentStatus status) {
        CustomsDocument doc = em.createQuery(
                "SELECT d FROM CustomsDocument d WHERE d.documentNumber = :docNum", CustomsDocument.class)
                .setParameter("docNum", docNum)
                .getResultStream()
                .findFirst()
                .orElse(null);

        if (doc != null) {
            doc.setStatus(status);
            em.merge(doc);
            em.flush();
            return doc.getId();
        }

        CustomsDocument newDoc = new CustomsDocument();
        newDoc.setDocumentNumber(docNum);
        newDoc.setDocumentType(CustomsDocumentType.IMPORT_DECLARATION);
        newDoc.setStatus(status);
        newDoc.setSubmissionDeadline(LocalDate.now().plusDays(5));
        newDoc = customsService.createCustomsDocument(newDoc, shipmentId, "TEST_ADMIN");
        return newDoc.getId();
    }

    // ── Diagnostic / admin operations ──

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public AutomationDiagnosticServiceBean.FullAutomationCycleSummary runFullAutomationCycle(String triggerSource) {
        return automationDiagnosticService.runFullAutomationCycle(triggerSource);
    }

    // ── Delegated automation worker/coordinator calls ──
    // These route through @RunAs(ADMIN) to propagate ADMIN caller identity to downstream EJBs.

    public boolean pollShipmentCarrier(Long shipmentId) {
        return trackingWorker.pollShipmentCarrier(shipmentId);
    }

    public ShipmentTrackingAutomationCoordinatorBean.ShipmentTrackingBatchSummary pollAllActiveShipments() {
        return trackingCoordinator.pollAllActiveShipments();
    }

    public InventoryReplenishmentAutomationWorkerBean.ReplenishmentEvaluationResult evaluateAndReplenish(Long itemId) {
        return replenishmentWorker.evaluateAndReplenish(itemId);
    }

    public InventoryReplenishmentAutomationCoordinatorBean.ReplenishmentBatchSummary evaluateAndReplenishAllItems() {
        return replenishmentCoordinator.evaluateAndReplenishAllItems();
    }

    public boolean pollCustomsDocumentStatus(Long documentId) {
        return customsWorker.pollCustomsDocumentStatus(documentId);
    }
}
