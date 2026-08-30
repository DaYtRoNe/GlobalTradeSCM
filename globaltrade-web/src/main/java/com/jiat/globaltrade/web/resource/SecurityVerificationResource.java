package com.jiat.globaltrade.web.resource;

import com.jiat.globaltrade.security.SecurityRoles;
import com.jiat.globaltrade.security.SecurityVerificationServiceBean;
import com.jiat.globaltrade.security.dto.CallerSecuritySummary;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBAccessException;
import jakarta.ejb.Stateless;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;

/**
 * REST endpoint resource for testing Phase 5A Authentication and Role-Based Access Control (RBAC).
 * Base path: /api/security
 *
 * Protected under web.xml BASIC authentication constraint (GlobalTradeRealm).
 * Delegates authorization decisions directly to the secured EJB (SecurityVerificationServiceBean).
 */
@Stateless
@Path("/security")
@Produces(MediaType.APPLICATION_JSON)
public class SecurityVerificationResource {

    @EJB
    private SecurityVerificationServiceBean securityService;

    /**
     * Inspects the authenticated caller principal and evaluated role memberships.
     * GET /api/security/whoami
     */
    @GET
    @Path("/whoami")
    public Response getCallerIdentity() {
        CallerSecuritySummary summary = securityService.getCallerSecurityInfo();

        JsonObjectBuilder rolesBuilder = Json.createObjectBuilder();
        for (Map.Entry<String, Boolean> entry : summary.getEvaluatedRoles().entrySet()) {
            rolesBuilder.add(entry.getKey(), entry.getValue());
        }

        JsonObject response = Json.createObjectBuilder()
                .add("principal", summary.getPrincipalName())
                .add("authenticated", summary.isAuthenticated())
                .add("authMechanism", summary.getAuthMechanism())
                .add("roles", rolesBuilder)
                .build();

        return Response.ok(response).build();
    }

    /**
     * Verifies administrative access guarded by @RolesAllowed(ADMIN).
     * POST /api/security/admin
     */
    @POST
    @Path("/admin")
    public Response testAdminAccess() {
        return executeGuardedOperation("adminOperation", SecurityRoles.ADMIN, () -> securityService.performAdminOperation());
    }

    /**
     * Verifies customs agent access guarded by @RolesAllowed({ADMIN, CUSTOMS_AGENT}).
     * POST /api/security/customs
     */
    @POST
    @Path("/customs")
    public Response testCustomsAccess() {
        return executeGuardedOperation("customsOperation", "ADMIN, CUSTOMS_AGENT", () -> securityService.performCustomsOperation());
    }

    /**
     * Verifies warehouse manager access guarded by @RolesAllowed({ADMIN, WAREHOUSE_MANAGER}).
     * POST /api/security/warehouse
     */
    @POST
    @Path("/warehouse")
    public Response testWarehouseAccess() {
        return executeGuardedOperation("warehouseOperation", "ADMIN, WAREHOUSE_MANAGER", () -> securityService.performWarehouseOperation());
    }

    /**
     * Verifies logistics coordinator access guarded by @RolesAllowed({ADMIN, LOGISTICS_COORDINATOR}).
     * POST /api/security/coordinator
     */
    @POST
    @Path("/coordinator")
    public Response testCoordinatorAccess() {
        return executeGuardedOperation("coordinatorOperation", "ADMIN, LOGISTICS_COORDINATOR", () -> securityService.performCoordinatorOperation());
    }

    /**
     * Verifies vendor representative access guarded by @RolesAllowed({ADMIN, VENDOR_REPRESENTATIVE}).
     * POST /api/security/vendor
     */
    @POST
    @Path("/vendor")
    public Response testVendorAccess() {
        return executeGuardedOperation("vendorOperation", "ADMIN, VENDOR_REPRESENTATIVE", () -> securityService.performVendorOperation());
    }

    /**
     * Verifies public information access guarded by @PermitAll.
     * GET /api/security/public
     */
    @GET
    @Path("/public")
    public Response testPublicAccess() {
        String result = securityService.getPublicInformation();
        JsonObject response = Json.createObjectBuilder()
                .add("status", "SUCCESS")
                .add("accessLevel", "PermitAll")
                .add("message", result)
                .build();
        return Response.ok(response).build();
    }

    /**
     * Verifies restricted internal access guarded by @DenyAll (always rejects).
     * POST /api/security/restricted
     */
    @POST
    @Path("/restricted")
    public Response testRestrictedAccess() {
        return executeGuardedOperation("restrictedInternalOperation", "NONE (@DenyAll)", () -> securityService.performRestrictedInternalOperation());
    }

    /**
     * Verifies programmatic authorization using SessionContext.isCallerInRole(...).
     * GET /api/security/programmatic
     */
    @GET
    @Path("/programmatic")
    public Response testProgrammaticCustomsReview() {
        boolean canReview = securityService.mayReviewInternationalCustomsData();
        CallerSecuritySummary caller = securityService.getCallerSecurityInfo();

        JsonObject response = Json.createObjectBuilder()
                .add("principal", caller.getPrincipalName())
                .add("programmaticCheck", "mayReviewInternationalCustomsData")
                .add("allowedRoles", "ADMIN or CUSTOMS_AGENT")
                .add("authorized", canReview)
                .add("message", canReview ?
                        "Caller is authorized to review international customs documentation." :
                        "Caller lacks clearance to review international customs documentation.")
                .build();

        return Response.ok(response).build();
    }

    @FunctionalInterface
    private interface SecurityAction {
        String run() throws Exception;
    }

    private Response executeGuardedOperation(String operationName, String requiredRoles, SecurityAction action) {
        try {
            String result = action.run();
            JsonObject response = Json.createObjectBuilder()
                    .add("status", "SUCCESS")
                    .add("operation", operationName)
                    .add("requiredRoles", requiredRoles)
                    .add("result", result)
                    .build();
            return Response.ok(response).build();
        } catch (EJBAccessException | SecurityException e) {
            JsonObject response = Json.createObjectBuilder()
                    .add("status", "FORBIDDEN")
                    .add("authorized", false)
                    .add("operation", operationName)
                    .add("requiredRoles", requiredRoles)
                    .add("message", "Access Denied: Caller does not possess the required security role for this operation.")
                    .build();
            return Response.status(Response.Status.FORBIDDEN).entity(response).build();
        } catch (Exception e) {
            // Check cause chain for EJBAccessException
            Throwable cause = e;
            while (cause != null) {
                if (cause instanceof EJBAccessException || cause instanceof SecurityException) {
                    JsonObject response = Json.createObjectBuilder()
                            .add("status", "FORBIDDEN")
                            .add("authorized", false)
                            .add("operation", operationName)
                            .add("requiredRoles", requiredRoles)
                            .add("message", "Access Denied: Caller does not possess the required security role for this operation.")
                            .build();
                    return Response.status(Response.Status.FORBIDDEN).entity(response).build();
                }
                cause = cause.getCause();
            }

            JsonObject response = Json.createObjectBuilder()
                    .add("status", "ERROR")
                    .add("message", e.getMessage() != null ? e.getMessage() : "Unknown error")
                    .build();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(response).build();
        }
    }
}
