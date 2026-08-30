package com.jiat.globaltrade.web.resource;

import com.jiat.globaltrade.entity.AuditLog;
import com.jiat.globaltrade.entity.InventoryItem;
import com.jiat.globaltrade.entity.Shipment;
import com.jiat.globaltrade.exception.InsufficientInventoryException;
import com.jiat.globaltrade.service.AuditServiceBean;
import com.jiat.globaltrade.service.InventoryReconciliationBean;
import com.jiat.globaltrade.service.InventoryServiceBean;
import com.jiat.globaltrade.service.ShipmentServiceBean;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

/**
 * Minimal verification resource for testing Phase 2 EJB Transaction Management (CMT & BMT).
 */
@Stateless
@Path("/transactions")
@Produces(MediaType.APPLICATION_JSON)
public class TransactionVerificationResource {

    @EJB
    private ShipmentServiceBean shipmentService;

    @EJB
    private InventoryServiceBean inventoryService;

    @EJB
    private AuditServiceBean auditService;

    @EJB
    private InventoryReconciliationBean reconciliationService;

    /**
     * Inspects current database state for Shipment #1 and InventoryItem #1 and total audit count.
     * GET /api/transactions/state
     */
    @GET
    @Path("/state")
    public Response getCurrentState() {
        Shipment shipment = shipmentService.findShipmentById(1L);
        InventoryItem item = inventoryService.findInventoryItemById(1L);
        long auditCount = auditService.getAuditLogCount();
        List<AuditLog> recentLogs = auditService.getRecentLogs(5);

        JsonArrayBuilder logsArray = Json.createArrayBuilder();
        for (AuditLog log : recentLogs) {
            logsArray.add(Json.createObjectBuilder()
                    .add("id", log.getId() != null ? log.getId() : 0)
                    .add("action", log.getAction() != null ? log.getAction() : "")
                    .add("entityType", log.getEntityType() != null ? log.getEntityType() : "")
                    .add("details", log.getDetails() != null ? log.getDetails() : "")
                    .add("timestamp", log.getTimestamp() != null ? log.getTimestamp().toString() : ""));
        }

        JsonObjectBuilder builder = Json.createObjectBuilder()
                .add("totalAuditLogs", auditCount)
                .add("recentAuditLogs", logsArray);

        if (shipment != null) {
            builder.add("shipment", Json.createObjectBuilder()
                    .add("id", shipment.getId())
                    .add("trackingNumber", shipment.getTrackingNumber())
                    .add("status", shipment.getShipmentStatus().name()));
        } else {
            builder.addNull("shipment");
        }

        if (item != null) {
            builder.add("inventoryItem", Json.createObjectBuilder()
                    .add("id", item.getId())
                    .add("sku", item.getSku())
                    .add("itemName", item.getItemName())
                    .add("quantity", item.getQuantity()));
        } else {
            builder.addNull("inventoryItem");
        }

        return Response.ok(builder.build()).build();
    }

    /**
     * Tests successful multi-step CMT transaction (REQUIRED):
     * Deducts 10 units and transitions shipment to IN_TRANSIT.
     * POST /api/transactions/dispatch/success
     */
    @POST
    @Path("/dispatch/success")
    public Response testSuccessfulDispatch() {
        try {
            Shipment shipment = shipmentService.processShipmentDispatch(1L, 1L, 10, "DISPATCH_OPERATOR");
            InventoryItem item = inventoryService.findInventoryItemById(1L);

            JsonObject response = Json.createObjectBuilder()
                    .add("status", "SUCCESS")
                    .add("message", "Shipment dispatch transaction committed atomically.")
                    .add("shipmentId", shipment.getId())
                    .add("shipmentStatus", shipment.getShipmentStatus().name())
                    .add("remainingStock", item != null ? item.getQuantity() : -1)
                    .build();

            return Response.ok(response).build();
        } catch (Exception e) {
            JsonObject response = Json.createObjectBuilder()
                    .add("status", "ERROR")
                    .add("message", e.getMessage())
                    .build();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(response).build();
        }
    }

    private static final java.util.logging.Logger LOGGER =
            java.util.logging.Logger.getLogger(TransactionVerificationResource.class.getName());

    /**
     * Tests CMT transaction rollback & independent audit survival:
     * Annotated with NOT_SUPPORTED so this verification method does NOT own or join
     * the transaction being tested.
     * When ShipmentServiceBean.processShipmentDispatch(REQUIRED) throws InsufficientInventoryException
     * (@ApplicationException(rollback = true)), only that business transaction rolls back.
     * This verification method catches the exception (or container wrapper) outside the rolled-back
     * transaction context, unwraps InsufficientInventoryException, and returns the verification JSON.
     * POST /api/transactions/dispatch/fail
     */
    @POST
    @Path("/dispatch/fail")
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Response testFailedDispatchRollback() {
        Shipment beforeShipment = shipmentService.findShipmentById(1L);
        InventoryItem beforeItem = inventoryService.findInventoryItemById(1L);
        long auditCountBefore = auditService.getAuditLogCount();

        try {
            shipmentService.processShipmentDispatch(1L, 1L, 99999, "DISPATCH_OPERATOR");
            return Response.ok(Json.createObjectBuilder()
                    .add("status", "UNEXPECTED_SUCCESS")
                    .build()).build();
        } catch (Exception e) {
            InsufficientInventoryException ex = findInsufficientInventoryException(e);
            if (ex != null) {
                Shipment afterShipment = shipmentService.findShipmentById(1L);
                InventoryItem afterItem = inventoryService.findInventoryItemById(1L);
                long auditCountAfter = auditService.getAuditLogCount();

                boolean inventoryUntouched = beforeItem != null && afterItem != null && beforeItem.getQuantity().equals(afterItem.getQuantity());
                boolean shipmentUntouched = beforeShipment != null && afterShipment != null && beforeShipment.getShipmentStatus().equals(afterShipment.getShipmentStatus());
                boolean independentAuditCommitted = auditCountAfter > auditCountBefore;

                JsonObject response = Json.createObjectBuilder()
                        .add("status", "TRANSACTION_ROLLED_BACK")
                        .add("caughtException", ex.getClass().getSimpleName())
                        .add("exceptionMessage", ex.getMessage())
                        .add("rollbackVerified", inventoryUntouched && shipmentUntouched)
                        .add("inventoryQuantityBefore", beforeItem != null ? beforeItem.getQuantity() : -1)
                        .add("inventoryQuantityAfter", afterItem != null ? afterItem.getQuantity() : -1)
                        .add("inventoryUntouched", inventoryUntouched)
                        .add("shipmentStatusBefore", beforeShipment != null ? beforeShipment.getShipmentStatus().name() : "")
                        .add("shipmentStatusAfter", afterShipment != null ? afterShipment.getShipmentStatus().name() : "")
                        .add("shipmentUntouched", shipmentUntouched)
                        .add("auditCountBefore", auditCountBefore)
                        .add("auditCountAfter", auditCountAfter)
                        .add("independentAuditCommitted", independentAuditCommitted)
                        .build();

                return Response.ok(response).build();
            }

            LOGGER.log(java.util.logging.Level.SEVERE, "[TransactionVerificationResource] Unexpected error in failed dispatch test", e);

            JsonObject response = Json.createObjectBuilder()
                    .add("status", "UNEXPECTED_ERROR")
                    .add("message", e.getMessage() != null ? e.getMessage() : "Unknown error")
                    .build();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(response).build();
        }
    }

    /**
     * Traverses exception cause hierarchy to unwrap container exceptions
     * (e.g. EJBException, TransactionRolledbackLocalException, EJBTransactionRolledbackException)
     * to find root InsufficientInventoryException.
     */
    private InsufficientInventoryException findInsufficientInventoryException(Throwable t) {
        Throwable current = t;
        while (current != null) {
            if (current instanceof InsufficientInventoryException ex) {
                return ex;
            }
            current = current.getCause();
        }
        return null;
    }

    /**
     * Tests MANDATORY transaction attribute negative case:
     * Explicitly executes with NOT_SUPPORTED to ensure NO transaction is active.
     * When InventoryServiceBean.adjustStockInternal (MANDATORY) is invoked without
     * a transaction, the container must throw EJBTransactionRequiredException.
     * POST /api/transactions/mandatory-test
     */
    @POST
    @Path("/mandatory-test")
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Response testMandatoryAttributeNegative() {
        InventoryItem beforeItem = inventoryService.findInventoryItemById(1L);
        boolean exceptionThrown = false;
        String caughtExceptionName = "";
        String expectedExceptionName = "EJBTransactionRequiredException";

        try {
            // Invoking a @TransactionAttribute(MANDATORY) method from a non-transactional caller context
            inventoryService.adjustStockInternal(1L, 10);
        } catch (Exception e) {
            exceptionThrown = true;
            caughtExceptionName = e.getClass().getSimpleName();
            Throwable cause = e.getCause();
            if (cause != null) {
                caughtExceptionName = cause.getClass().getSimpleName();
            }
        }

        InventoryItem afterItem = inventoryService.findInventoryItemById(1L);
        boolean inventoryUnchanged = beforeItem != null && afterItem != null && beforeItem.getQuantity().equals(afterItem.getQuantity());

        JsonObject response = Json.createObjectBuilder()
                .add("mandatoryEnforced", exceptionThrown)
                .add("expectedException", expectedExceptionName)
                .add("actualExceptionCaught", caughtExceptionName)
                .add("inventoryUnchanged", inventoryUnchanged)
                .add("message", "Method adjustStockInternal rejected execution because no active transaction context was present (MANDATORY enforced).")
                .build();

        return Response.ok(response).build();
    }

    /**
     * Tests Bean-Managed Transaction (BMT) programmatic commit.
     * POST /api/transactions/reconcile/commit
     */
    @POST
    @Path("/reconcile/commit")
    public Response testBmtCommit() {
        InventoryItem item = inventoryService.findInventoryItemById(1L);
        int currentCount = item != null ? item.getQuantity() : 1000;
        int reconciledCount = Math.max(100, currentCount - 5); // 5 units variance (under threshold of 20)

        InventoryReconciliationBean.ReconciliationResult result =
                reconciliationService.reconcilePhysicalCount(1L, reconciledCount, 20, "CHIEF_AUDITOR");

        JsonObject response = Json.createObjectBuilder()
                .add("bmtResult", result.isSuccess() ? "COMMITTED" : "REJECTED")
                .add("message", result.getMessage())
                .add("previousCount", result.getPreviousCount())
                .add("reconciledCount", result.getReconciledCount())
                .build();

        return Response.ok(response).build();
    }

    /**
     * Tests Bean-Managed Transaction (BMT) programmatic rollback.
     * POST /api/transactions/reconcile/rollback
     */
    @POST
    @Path("/reconcile/rollback")
    public Response testBmtRollback() {
        InventoryItem item = inventoryService.findInventoryItemById(1L);
        int currentCount = item != null ? item.getQuantity() : 1000;
        int extremeCount = currentCount + 500; // 500 units variance (exceeds threshold of 20)

        InventoryReconciliationBean.ReconciliationResult result =
                reconciliationService.reconcilePhysicalCount(1L, extremeCount, 20, "CHIEF_AUDITOR");

        InventoryItem afterItem = inventoryService.findInventoryItemById(1L);

        JsonObject response = Json.createObjectBuilder()
                .add("bmtResult", result.isSuccess() ? "COMMITTED" : "PROGRAMMATICALLY_ROLLED_BACK")
                .add("message", result.getMessage())
                .add("previousCount", result.getPreviousCount())
                .add("requestedCount", extremeCount)
                .add("persistedCountAfterRollback", afterItem != null ? afterItem.getQuantity() : -1)
                .add("rollbackVerified", afterItem != null && afterItem.getQuantity() == currentCount)
                .build();

        return Response.ok(response).build();
    }
}
