package com.jiat.globaltrade.web.resource;

import com.jiat.globaltrade.entity.RouteOptimizationRecommendation;
import com.jiat.globaltrade.exception.ResourceNotFoundException;
import com.jiat.globaltrade.security.SecurityRoles;
import com.jiat.globaltrade.service.RouteOptimizationCoordinatorBean;
import com.jiat.globaltrade.service.RouteOptimizationCoordinatorBean.RouteOptimizationBatchSummary;
import com.jiat.globaltrade.service.RouteOptimizationServiceBean;
import com.jiat.globaltrade.service.RouteOptimizationWorkerBean.SingleShipmentOptimizationResult;
import com.jiat.globaltrade.web.dto.RouteOptimizationResponse;
import jakarta.annotation.Resource;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.SessionContext;
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
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Production REST Resource for Route Optimization Recommendations and Manual Triggers.
 * Base Path: /api/route-optimizations
 */
@Stateless
@Path("/route-optimizations")
@Produces(MediaType.APPLICATION_JSON)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@DeclareRoles({
        SecurityRoles.ADMIN,
        SecurityRoles.LOGISTICS_COORDINATOR
})
public class RouteOptimizationResource {

    @EJB
    private RouteOptimizationServiceBean routeOptimizationService;

    @EJB
    private RouteOptimizationCoordinatorBean coordinatorBean;

    @Resource
    private SessionContext sessionContext;

    /**
     * Lists all current active route recommendations.
     * GET /api/route-optimizations
     */
    @GET
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.LOGISTICS_COORDINATOR})
    public Response getAllRecommendations() {
        List<RouteOptimizationRecommendation> recs = routeOptimizationService.findAllRecommendations();
        List<RouteOptimizationResponse> response = recs.stream()
                .map(RouteOptimizationResponse::fromEntity)
                .collect(Collectors.toList());

        return Response.ok(response).build();
    }

    /**
     * Retrieves the current route recommendation for a specific shipment.
     * GET /api/route-optimizations/shipment/{shipmentId}
     */
    @GET
    @Path("/shipment/{shipmentId}")
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.LOGISTICS_COORDINATOR})
    public Response getRecommendationForShipment(@PathParam("shipmentId") Long shipmentId) {
        try {
            RouteOptimizationRecommendation rec = routeOptimizationService.findRecommendationByShipmentId(shipmentId);
            return Response.ok(RouteOptimizationResponse.fromEntity(rec)).build();
        } catch (ResourceNotFoundException e) {
            JsonObject error = Json.createObjectBuilder()
                    .add("status", "NOT_FOUND")
                    .add("message", e.getMessage())
                    .build();
            return Response.status(Response.Status.NOT_FOUND).entity(error).build();
        }
    }

    /**
     * Manually triggers route optimization evaluation for a specific shipment.
     * POST /api/route-optimizations/shipment/{shipmentId}/evaluate
     */
    @POST
    @Path("/shipment/{shipmentId}/evaluate")
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.LOGISTICS_COORDINATOR})
    public Response evaluateShipmentRoute(@PathParam("shipmentId") Long shipmentId) {
        String caller = sessionContext.getCallerPrincipal() != null ?
                sessionContext.getCallerPrincipal().getName() : "ANONYMOUS";

        try {
            RouteOptimizationRecommendation rec =
                    routeOptimizationService.optimizeShipmentRoute(shipmentId, "MANUAL_REST_TRIGGER", caller);

            if (rec == null) {
                JsonObject response = Json.createObjectBuilder()
                        .add("status", "SKIPPED")
                        .add("shipmentId", shipmentId)
                        .add("message", "Shipment is not in an active operational status (DELIVERED/CANCELLED).")
                        .build();
                return Response.status(Response.Status.BAD_REQUEST).entity(response).build();
            }

            return Response.ok(RouteOptimizationResponse.fromEntity(rec)).build();
        } catch (ResourceNotFoundException e) {
            JsonObject error = Json.createObjectBuilder()
                    .add("status", "NOT_FOUND")
                    .add("message", e.getMessage())
                    .build();
            return Response.status(Response.Status.NOT_FOUND).entity(error).build();
        } catch (Exception e) {
            JsonObject error = Json.createObjectBuilder()
                    .add("status", "ERROR")
                    .add("message", e.getMessage())
                    .build();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
        }
    }

    /**
     * Manually executes batch route optimization for all active shipments (ADMIN only).
     * POST /api/route-optimizations/run
     */
    @POST
    @Path("/run")
    @RolesAllowed(SecurityRoles.ADMIN)
    public Response runBatchRouteOptimization() {
        String caller = sessionContext.getCallerPrincipal() != null ?
                sessionContext.getCallerPrincipal().getName() : "ADMIN";

        RouteOptimizationBatchSummary summary =
                coordinatorBean.optimizeAllActiveShipments("MANUAL_REST_BATCH_TRIGGER", caller);

        JsonArrayBuilder detailsArray = Json.createArrayBuilder();
        for (SingleShipmentOptimizationResult r : summary.getResults()) {
            JsonObjectBuilder b = Json.createObjectBuilder()
                    .add("shipmentId", r.getShipmentId())
                    .add("success", r.isSuccess())
                    .add("skipped", r.isSkipped());
            if (r.getRouteCode() != null) b.add("routeCode", r.getRouteCode());
            if (r.getScore() != null) b.add("score", r.getScore().toString());
            if (r.getTransitHours() != null) b.add("transitHours", r.getTransitHours());
            if (r.getEstimatedCost() != null) b.add("estimatedCost", r.getEstimatedCost().toString());
            if (r.getErrorMessage() != null) b.add("errorMessage", r.getErrorMessage());
            detailsArray.add(b);
        }

        JsonObject response = Json.createObjectBuilder()
                .add("status", summary.getOverallStatus())
                .add("triggerSource", summary.getTriggerSource())
                .add("executionTime", summary.getExecutionTime().toString())
                .add("totalShipmentsEvaluated", summary.getTotalShipmentsEvaluated())
                .add("successfulOptimizations", summary.getSuccessfulOptimizations())
                .add("failedOptimizations", summary.getFailedOptimizations())
                .add("skippedShipments", summary.getSkippedShipments())
                .add("results", detailsArray)
                .build();

        return Response.ok(response).build();
    }
}
