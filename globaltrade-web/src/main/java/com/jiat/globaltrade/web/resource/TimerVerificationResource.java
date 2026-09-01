package com.jiat.globaltrade.web.resource;

import com.jiat.globaltrade.security.SecurityRoles;
import com.jiat.globaltrade.service.AuditServiceBean;
import com.jiat.globaltrade.service.SupplyChainMonitoringServiceBean;
import com.jiat.globaltrade.service.SupplyChainMonitoringWorkerBean;
import com.jiat.globaltrade.timer.ShipmentAlertTimerBean;
import com.jiat.globaltrade.timer.SupplyChainMonitoringTimerBean;
import com.jiat.globaltrade.timer.dto.AlertTimerInfo;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

/**
 * Diagnostic verification resource for testing EJB Timer Services (Declarative & Programmatic).
 * Base Path: /api/timers
 */
@Stateless
@Path("/timers")
@Produces(MediaType.APPLICATION_JSON)
@DeclareRoles({
        SecurityRoles.ADMIN,
        SecurityRoles.LOGISTICS_COORDINATOR,
        SecurityRoles.CUSTOMS_AGENT,
        SecurityRoles.WAREHOUSE_MANAGER,
        SecurityRoles.VENDOR_REPRESENTATIVE,
        SecurityRoles.CUSTOMER
})
public class TimerVerificationResource {

    @EJB
    private SupplyChainMonitoringTimerBean monitoringTimerBean;

    @EJB
    private ShipmentAlertTimerBean alertTimerBean;

    @EJB
    private AuditServiceBean auditService;

    /**
     * Inspects current runtime status of declarative and programmatic timers.
     * GET /api/timers/status
     */
    @GET
    @Path("/status")
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.LOGISTICS_COORDINATOR})
    public Response getTimerStatus() {
        List<AlertTimerInfo> activeTimers = alertTimerBean.getActiveTimers();
        long auditCount = auditService.getAuditLogCount();

        JsonArrayBuilder timersArray = Json.createArrayBuilder();
        for (AlertTimerInfo info : activeTimers) {
            timersArray.add(Json.createObjectBuilder()
                    .add("alertType", info.getAlertType())
                    .add("targetId", info.getTargetId())
                    .add("referenceCode", info.getReferenceCode() != null ? info.getReferenceCode() : "")
                    .add("reason", info.getReason() != null ? info.getReason() : "")
                    .add("delayMillis", info.getDelayMillis())
                    .add("scheduledAt", info.getScheduledAt() != null ? info.getScheduledAt().toString() : ""));
        }

        JsonObjectBuilder builder = Json.createObjectBuilder()
                .add("declarativeTimerConfigured", true)
                .add("scheduleExpression", "hour='*', minute='*/5', second='0', persistent=true")
                .add("monitoringCycleCount", monitoringTimerBean.getMonitoringCycleCount())
                .add("lastMonitoringTime", monitoringTimerBean.getLastMonitoringTime() != null ?
                        monitoringTimerBean.getLastMonitoringTime().toString() : "Not yet executed")
                .add("lastOverallStatus", monitoringTimerBean.getLastOverallStatus())
                .add("lastSuccessfulCategories", monitoringTimerBean.getLastSuccessfulCategories())
                .add("lastFailedCategories", monitoringTimerBean.getLastFailedCategories())
                .add("lastActiveAlertsCount", monitoringTimerBean.getLastActiveAlertsCount())
                .add("lastResolvedAlertsCount", monitoringTimerBean.getLastResolvedAlertsCount())
                .add("activeProgrammaticTimerCount", activeTimers.size())
                .add("activeProgrammaticTimers", timersArray)
                .add("totalAuditLogs", auditCount);

        return Response.ok(builder.build()).build();
    }

    /**
     * Manually invokes the monitoring cycle logic without waiting for the 5-minute @Schedule interval.
     * Restricted strictly to ADMIN users.
     * POST /api/timers/run-monitoring
     */
    @POST
    @Path("/run-monitoring")
    @RolesAllowed(SecurityRoles.ADMIN)
    public Response runMonitoringManually() {
        SupplyChainMonitoringServiceBean.SupplyChainEvaluationResult summary =
                monitoringTimerBean.runMonitoringCycle("MANUAL_REST_TRIGGER");

        JsonArrayBuilder categoriesArray = Json.createArrayBuilder();
        for (SupplyChainMonitoringWorkerBean.CategoryResult cr : summary.getCategoryResults()) {
            categoriesArray.add(Json.createObjectBuilder()
                    .add("category", cr.getCategoryName())
                    .add("success", cr.isSuccess())
                    .add("errorMessage", cr.getErrorMessage() != null ? cr.getErrorMessage() : "")
                    .add("entitiesEvaluated", cr.getEntitiesEvaluated())
                    .add("activeAlertsDetected", cr.getActiveAlertsDetected())
                    .add("alertsResolved", cr.getAlertsResolved()));
        }

        JsonObject response = Json.createObjectBuilder()
                .add("status", summary.getOverallStatus())
                .add("triggerSource", summary.getTriggerSource())
                .add("executionTime", summary.getExecutionTime().toString())
                .add("successfulCategories", summary.getSuccessfulCategories())
                .add("failedCategories", summary.getFailedCategories())
                .add("totalEntitiesEvaluated", summary.getTotalEntitiesEvaluated())
                .add("totalActiveAlertsDetected", summary.getTotalActiveAlertsDetected())
                .add("totalAlertsResolved", summary.getTotalAlertsResolved())
                .add("categories", categoriesArray)
                .build();

        return Response.ok(response).build();
    }

    /**
     * Programmatically schedules a single-action shipment expedite alert.
     * POST /api/timers/shipment-alert/{shipmentId}?delaySeconds=5&reason=Expedite+transit
     */
    @POST
    @Path("/shipment-alert/{shipmentId}")
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.LOGISTICS_COORDINATOR})
    public Response scheduleShipmentAlert(
            @PathParam("shipmentId") Long shipmentId,
            @QueryParam("delaySeconds") @DefaultValue("5") long delaySeconds,
            @QueryParam("reason") @DefaultValue("Expedite transit check") String reason) {

        try {
            long delayMillis = delaySeconds * 1000L;
            AlertTimerInfo timerInfo = alertTimerBean.scheduleShipmentAlert(shipmentId, delayMillis, reason);

            JsonObject response = Json.createObjectBuilder()
                    .add("status", "SCHEDULED")
                    .add("alertType", timerInfo.getAlertType())
                    .add("shipmentId", timerInfo.getTargetId())
                    .add("trackingNumber", timerInfo.getReferenceCode())
                    .add("reason", timerInfo.getReason())
                    .add("delaySeconds", delaySeconds)
                    .add("delayMillis", delayMillis)
                    .add("scheduledAt", timerInfo.getScheduledAt().toString())
                    .add("message", String.format("Programmatic timer will fire in %d seconds.", delaySeconds))
                    .build();

            return Response.ok(response).build();
        } catch (Exception e) {
            JsonObject response = Json.createObjectBuilder()
                    .add("status", "ERROR")
                    .add("message", e.getMessage())
                    .build();
            return Response.status(Response.Status.BAD_REQUEST).entity(response).build();
        }
    }

    /**
     * Cancels an active programmatic shipment alert timer.
     * DELETE /api/timers/shipment-alert/{shipmentId}
     */
    @DELETE
    @Path("/shipment-alert/{shipmentId}")
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.LOGISTICS_COORDINATOR})
    public Response cancelShipmentAlert(@PathParam("shipmentId") Long shipmentId) {
        boolean cancelled = alertTimerBean.cancelShipmentAlert(shipmentId);

        JsonObject response = Json.createObjectBuilder()
                .add("shipmentId", shipmentId)
                .add("cancelled", cancelled)
                .add("message", cancelled ? "Programmatic timer cancelled successfully." : "No active timer found for this shipment.")
                .build();

        return Response.ok(response).build();
    }

    /**
     * Programmatically schedules a single-action customs filing reminder.
     * POST /api/timers/customs-reminder/{documentId}?delaySeconds=5&reason=Clearance+deadline
     */
    @POST
    @Path("/customs-reminder/{documentId}")
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.CUSTOMS_AGENT})
    public Response scheduleCustomsReminder(
            @PathParam("documentId") Long documentId,
            @QueryParam("delaySeconds") @DefaultValue("5") long delaySeconds,
            @QueryParam("reason") @DefaultValue("Customs filing deadline alert") String reason) {

        try {
            long delayMillis = delaySeconds * 1000L;
            AlertTimerInfo timerInfo = alertTimerBean.scheduleCustomsReminder(documentId, delayMillis, reason);

            JsonObject response = Json.createObjectBuilder()
                    .add("status", "SCHEDULED")
                    .add("alertType", timerInfo.getAlertType())
                    .add("documentId", timerInfo.getTargetId())
                    .add("documentNumber", timerInfo.getReferenceCode())
                    .add("reason", timerInfo.getReason())
                    .add("delaySeconds", delaySeconds)
                    .add("delayMillis", delayMillis)
                    .add("scheduledAt", timerInfo.getScheduledAt().toString())
                    .add("message", String.format("Programmatic customs timer will fire in %d seconds.", delaySeconds))
                    .build();

            return Response.ok(response).build();
        } catch (Exception e) {
            JsonObject response = Json.createObjectBuilder()
                    .add("status", "ERROR")
                    .add("message", e.getMessage())
                    .build();
            return Response.status(Response.Status.BAD_REQUEST).entity(response).build();
        }
    }

    /**
     * Cancels an active programmatic customs reminder timer.
     * DELETE /api/timers/customs-reminder/{documentId}
     */
    @DELETE
    @Path("/customs-reminder/{documentId}")
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.CUSTOMS_AGENT})
    public Response cancelCustomsReminder(@PathParam("documentId") Long documentId) {
        boolean cancelled = alertTimerBean.cancelCustomsReminder(documentId);

        JsonObject response = Json.createObjectBuilder()
                .add("documentId", documentId)
                .add("cancelled", cancelled)
                .add("message", cancelled ? "Programmatic customs timer cancelled successfully." : "No active timer found for this document.")
                .build();

        return Response.ok(response).build();
    }
}
