package com.jiat.globaltrade.web.resource;

import com.jiat.globaltrade.entity.CustomsDocument;
import com.jiat.globaltrade.entity.InventoryItem;
import com.jiat.globaltrade.entity.Shipment;
import com.jiat.globaltrade.entity.Vendor;
import com.jiat.globaltrade.entity.enums.CustomsDocumentStatus;
import com.jiat.globaltrade.exception.VendorAccessDeniedException;
import com.jiat.globaltrade.security.VendorAuthorizationServiceBean;
import com.jiat.globaltrade.service.CustomsServiceBean;
import com.jiat.globaltrade.service.InventoryServiceBean;
import com.jiat.globaltrade.service.ShipmentServiceBean;
import jakarta.ejb.AccessLocalException;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBAccessException;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REST Endpoint Resource demonstrating real business-service RBAC and fine-grained authorization.
 * Base Path: /api/business-security
 *
 * Protected under web.xml BASIC authentication constraint (GlobalTradeRealm).
 * Configured with @TransactionAttribute(NOT_SUPPORTED) so the REST facade does not initiate
 * or own an outer transaction, allowing downstream business EJBs to independently manage
 * their CMT transactions and ensuring security denials translate cleanly to HTTP 403.
 */
@Stateless
@Path("/business-security")
@Produces(MediaType.APPLICATION_JSON)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class BusinessSecurityVerificationResource {

    private static final Logger LOGGER = Logger.getLogger(BusinessSecurityVerificationResource.class.getName());

    @EJB
    private VendorAuthorizationServiceBean vendorAuthService;

    @EJB
    private CustomsServiceBean customsService;

    @EJB
    private InventoryServiceBean inventoryService;

    @EJB
    private ShipmentServiceBean shipmentService;

    /**
     * Demonstrates fine-grained vendor data authorization.
     * GET /api/business-security/vendor/{id}
     */
    @GET
    @Path("/vendor/{id}")
    public Response getSecuredVendor(@PathParam("id") Long id, @Context SecurityContext securityContext) {
        String caller = securityContext.getUserPrincipal() != null ? securityContext.getUserPrincipal().getName() : "ANONYMOUS";
        try {
            Vendor vendor = vendorAuthService.getVendorForAuthorizedCaller(id);
            if (vendor == null) {
                JsonObject notFound = Json.createObjectBuilder()
                        .add("status", "NOT_FOUND")
                        .add("message", "Vendor not found for ID: " + id)
                        .build();
                return Response.status(Response.Status.NOT_FOUND).entity(notFound).build();
            }

            JsonObject response = Json.createObjectBuilder()
                    .add("status", "SUCCESS")
                    .add("authorizedCaller", caller)
                    .add("vendorId", vendor.getId())
                    .add("vendorCode", vendor.getVendorCode())
                    .add("companyName", vendor.getCompanyName())
                    .add("country", vendor.getCountry())
                    .add("status", vendor.getStatus().name())
                    .add("performanceRating", vendor.getPerformanceRating() != null ? vendor.getPerformanceRating().doubleValue() : 0.0)
                    .build();

            return Response.ok(response).build();
        } catch (Exception e) {
            if (isAuthorizationException(e)) {
                LOGGER.log(Level.WARNING, "[BusinessSecurityVerificationResource] Access denied to Vendor #{0} for caller: {1}",
                        new Object[]{id, caller});
                JsonObject forbidden = Json.createObjectBuilder()
                        .add("status", "FORBIDDEN")
                        .add("authorized", false)
                        .add("caller", caller)
                        .add("targetVendorId", id)
                        .add("message", "Access denied to the requested vendor.")
                        .build();
                return Response.status(Response.Status.FORBIDDEN).entity(forbidden).build();
            }

            LOGGER.log(Level.SEVERE, "[BusinessSecurityVerificationResource] Unexpected error in getSecuredVendor", e);
            JsonObject serverError = Json.createObjectBuilder()
                    .add("status", "ERROR")
                    .add("message", "An unexpected error occurred while processing the request.")
                    .build();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(serverError).build();
        }
    }

    /**
     * Demonstrates inventory item lookup.
     * GET /api/business-security/inventory/{id}
     */
    @GET
    @Path("/inventory/{id}")
    public Response getInventoryItem(@PathParam("id") Long id, @Context SecurityContext securityContext) {
        String caller = securityContext.getUserPrincipal() != null ? securityContext.getUserPrincipal().getName() : "ANONYMOUS";
        try {
            InventoryItem item = inventoryService.findInventoryItemById(id);
            if (item == null) {
                JsonObject notFound = Json.createObjectBuilder()
                        .add("status", "NOT_FOUND")
                        .add("message", "Inventory item not found for ID: " + id)
                        .build();
                return Response.status(Response.Status.NOT_FOUND).entity(notFound).build();
            }

            JsonObject response = Json.createObjectBuilder()
                    .add("status", "SUCCESS")
                    .add("caller", caller)
                    .add("itemId", item.getId())
                    .add("sku", item.getSku())
                    .add("itemName", item.getItemName())
                    .add("quantity", item.getQuantity())
                    .add("reorderLevel", item.getReorderLevel())
                    .add("unitPrice", item.getUnitPrice() != null ? item.getUnitPrice().doubleValue() : 0.0)
                    .build();

            return Response.ok(response).build();
        } catch (Exception e) {
            if (isAuthorizationException(e)) {
                JsonObject forbidden = Json.createObjectBuilder()
                        .add("status", "FORBIDDEN")
                        .add("authorized", false)
                        .add("caller", caller)
                        .add("message", "Access denied: Caller lacks clearance to access inventory records.")
                        .build();
                return Response.status(Response.Status.FORBIDDEN).entity(forbidden).build();
            }

            LOGGER.log(Level.SEVERE, "[BusinessSecurityVerificationResource] Unexpected error in getInventoryItem", e);
            JsonObject serverError = Json.createObjectBuilder()
                    .add("status", "ERROR")
                    .add("message", "An unexpected error occurred while processing the request.")
                    .build();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(serverError).build();
        }
    }

    /**
     * Demonstrates customs document review / clearance status update.
     * Requires @RolesAllowed({ADMIN, CUSTOMS_AGENT}).
     * POST /api/business-security/customs/{id}/review
     */
    @POST
    @Path("/customs/{id}/review")
    public Response reviewCustomsDocument(@PathParam("id") Long id, @Context SecurityContext securityContext) {
        String caller = securityContext.getUserPrincipal() != null ? securityContext.getUserPrincipal().getName() : "ANONYMOUS";
        try {
            CustomsDocument doc = customsService.updateDocumentStatus(id, CustomsDocumentStatus.APPROVED, caller);
            if (doc == null) {
                JsonObject notFound = Json.createObjectBuilder()
                        .add("status", "NOT_FOUND")
                        .add("message", "Customs document not found for ID: " + id)
                        .build();
                return Response.status(Response.Status.NOT_FOUND).entity(notFound).build();
            }

            JsonObject response = Json.createObjectBuilder()
                    .add("status", "SUCCESS")
                    .add("operation", "REVIEW_CUSTOMS_DOCUMENT")
                    .add("requiredRoles", "ADMIN, CUSTOMS_AGENT")
                    .add("caller", caller)
                    .add("documentId", doc.getId())
                    .add("documentNumber", doc.getDocumentNumber())
                    .add("newStatus", doc.getStatus().name())
                    .build();

            return Response.ok(response).build();
        } catch (Exception e) {
            if (isAuthorizationException(e)) {
                JsonObject forbidden = Json.createObjectBuilder()
                        .add("status", "FORBIDDEN")
                        .add("authorized", false)
                        .add("operation", "REVIEW_CUSTOMS_DOCUMENT")
                        .add("requiredRoles", "ADMIN, CUSTOMS_AGENT")
                        .add("caller", caller)
                        .add("message", "Access Denied: Caller does not possess the required customs clearance role.")
                        .build();
                return Response.status(Response.Status.FORBIDDEN).entity(forbidden).build();
            }

            LOGGER.log(Level.SEVERE, "[BusinessSecurityVerificationResource] Unexpected error in reviewCustomsDocument", e);
            JsonObject serverError = Json.createObjectBuilder()
                    .add("status", "ERROR")
                    .add("message", "An unexpected error occurred while processing the request.")
                    .build();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(serverError).build();
        }
    }

    /**
     * Demonstrates warehouse stock replenishment.
     * Requires @RolesAllowed({ADMIN, WAREHOUSE_MANAGER}).
     * POST /api/business-security/inventory/{id}/replenish?quantity=100
     */
    @POST
    @Path("/inventory/{id}/replenish")
    public Response replenishInventoryStock(@PathParam("id") Long id,
                                            @QueryParam("quantity") @DefaultValue("50") int quantity,
                                            @Context SecurityContext securityContext) {
        String caller = securityContext.getUserPrincipal() != null ? securityContext.getUserPrincipal().getName() : "ANONYMOUS";
        try {
            InventoryItem item = inventoryService.increaseStock(id, quantity, caller);
            if (item == null) {
                JsonObject notFound = Json.createObjectBuilder()
                        .add("status", "NOT_FOUND")
                        .add("message", "Inventory item not found for ID: " + id)
                        .build();
                return Response.status(Response.Status.NOT_FOUND).entity(notFound).build();
            }

            JsonObject response = Json.createObjectBuilder()
                    .add("status", "SUCCESS")
                    .add("operation", "REPLENISH_INVENTORY_STOCK")
                    .add("requiredRoles", "ADMIN, WAREHOUSE_MANAGER")
                    .add("caller", caller)
                    .add("itemId", item.getId())
                    .add("sku", item.getSku())
                    .add("replenishedUnits", quantity)
                    .add("newQuantity", item.getQuantity())
                    .build();

            return Response.ok(response).build();
        } catch (Exception e) {
            if (isAuthorizationException(e)) {
                JsonObject forbidden = Json.createObjectBuilder()
                        .add("status", "FORBIDDEN")
                        .add("authorized", false)
                        .add("operation", "REPLENISH_INVENTORY_STOCK")
                        .add("requiredRoles", "ADMIN, WAREHOUSE_MANAGER")
                        .add("caller", caller)
                        .add("message", "Access Denied: Caller does not possess the required warehouse management role.")
                        .build();
                return Response.status(Response.Status.FORBIDDEN).entity(forbidden).build();
            }

            LOGGER.log(Level.SEVERE, "[BusinessSecurityVerificationResource] Unexpected error in replenishInventoryStock", e);
            JsonObject serverError = Json.createObjectBuilder()
                    .add("status", "ERROR")
                    .add("message", "An unexpected error occurred while processing the request.")
                    .build();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(serverError).build();
        }
    }

    /**
     * Demonstrates shipment dispatch orchestration under RBAC.
     * Requires @RolesAllowed({ADMIN, LOGISTICS_COORDINATOR, WAREHOUSE_MANAGER}).
     * POST /api/business-security/shipment/{id}/dispatch?inventoryId=1&quantity=10
     */
    @POST
    @Path("/shipment/{id}/dispatch")
    public Response dispatchShipment(@PathParam("id") Long id,
                                     @QueryParam("inventoryId") @DefaultValue("1") Long inventoryId,
                                     @QueryParam("quantity") @DefaultValue("10") int quantity,
                                     @Context SecurityContext securityContext) {
        String caller = securityContext.getUserPrincipal() != null ? securityContext.getUserPrincipal().getName() : "ANONYMOUS";
        try {
            Shipment shipment = shipmentService.processShipmentDispatch(id, inventoryId, quantity, caller);
            JsonObject response = Json.createObjectBuilder()
                    .add("status", "SUCCESS")
                    .add("operation", "PROCESS_SHIPMENT_DISPATCH")
                    .add("requiredRoles", "ADMIN, LOGISTICS_COORDINATOR, WAREHOUSE_MANAGER")
                    .add("caller", caller)
                    .add("shipmentId", shipment.getId())
                    .add("trackingNumber", shipment.getTrackingNumber())
                    .add("newShipmentStatus", shipment.getShipmentStatus().name())
                    .build();

            return Response.ok(response).build();
        } catch (Exception e) {
            if (isAuthorizationException(e)) {
                JsonObject forbidden = Json.createObjectBuilder()
                        .add("status", "FORBIDDEN")
                        .add("authorized", false)
                        .add("operation", "PROCESS_SHIPMENT_DISPATCH")
                        .add("requiredRoles", "ADMIN, LOGISTICS_COORDINATOR, WAREHOUSE_MANAGER")
                        .add("caller", caller)
                        .add("message", "Access Denied: Caller does not possess the required shipment dispatch role.")
                        .build();
                return Response.status(Response.Status.FORBIDDEN).entity(forbidden).build();
            }

            LOGGER.log(Level.SEVERE, "[BusinessSecurityVerificationResource] Error in dispatchShipment", e);
            JsonObject error = Json.createObjectBuilder()
                    .add("status", "ERROR")
                    .add("caller", caller)
                    .add("message", e.getMessage() != null ? e.getMessage() : "Unknown error during dispatch")
                    .build();
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        }
    }

    /**
     * Traverses the exception cause hierarchy to identify if an authorization or permission denial occurred.
     */
    private boolean isAuthorizationException(Throwable t) {
        Throwable current = t;
        while (current != null) {
            if (current instanceof VendorAccessDeniedException
                    || current instanceof EJBAccessException
                    || current instanceof AccessLocalException
                    || current instanceof SecurityException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
