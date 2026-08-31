package com.jiat.globaltrade.web.resource;

import com.jiat.globaltrade.exception.BusinessRuleViolationException;
import com.jiat.globaltrade.exception.InsufficientInventoryException;
import com.jiat.globaltrade.exception.ResourceNotFoundException;
import com.jiat.globaltrade.security.SecurityRoles;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Diagnostic verification resource for testing Phase 6 Advanced EJB Exception Handling.
 * Demonstrates:
 * - 404 ResourceNotFoundException mapping
 * - 400 Validation / IllegalArgumentException mapping
 * - 409 InsufficientInventoryException mapping
 * - 400 BusinessRuleViolationException mapping
 * - 500 Safe generic system exception masking (zero stack traces leaked)
 *
 * Secured with @RolesAllowed(ADMIN) and HTTP BASIC authentication under GlobalTradeCustomRealm.
 */
@Stateless
@Path("/exceptions")
@Produces(MediaType.APPLICATION_JSON)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@DeclareRoles({SecurityRoles.ADMIN})
@RolesAllowed(SecurityRoles.ADMIN)
public class ExceptionVerificationResource {

    private static final Logger LOGGER = Logger.getLogger(ExceptionVerificationResource.class.getName());

    /**
     * Demonstrates checked application exception ResourceNotFoundException -> HTTP 404.
     * GET /api/exceptions/not-found?type=Vendor&id=9999
     */
    @GET
    @Path("/not-found")
    public Response testResourceNotFound(@QueryParam("type") @DefaultValue("Vendor") String type,
                                         @QueryParam("id") @DefaultValue("9999") Long id) throws ResourceNotFoundException {
        LOGGER.log(Level.INFO, "[ExceptionVerificationResource] Triggering ResourceNotFoundException for {0} ID {1}",
                new Object[]{type, id});
        throw new ResourceNotFoundException(type, id);
    }

    /**
     * Demonstrates input validation / compliance parameter check failure -> HTTP 400.
     * POST /api/exceptions/validation
     */
    @POST
    @Path("/validation")
    public Response testValidationFailure() {
        LOGGER.log(Level.INFO, "[ExceptionVerificationResource] Triggering validation failure IllegalArgumentException");
        throw new IllegalArgumentException("Validation failed: Performance rating (9.99) must be between 0.00 and 5.00.");
    }

    /**
     * Demonstrates business conflict InsufficientInventoryException -> HTTP 409.
     * POST /api/exceptions/inventory-conflict
     */
    @POST
    @Path("/inventory-conflict")
    public Response testInventoryConflict() throws InsufficientInventoryException {
        LOGGER.log(Level.INFO, "[ExceptionVerificationResource] Triggering InsufficientInventoryException");
        throw new InsufficientInventoryException(1L, 50000, 100);
    }

    /**
     * Demonstrates business rule violation -> HTTP 400.
     * POST /api/exceptions/business-rule
     */
    @POST
    @Path("/business-rule")
    public Response testBusinessRuleViolation() throws BusinessRuleViolationException {
        LOGGER.log(Level.INFO, "[ExceptionVerificationResource] Triggering BusinessRuleViolationException");
        throw new BusinessRuleViolationException("INVALID_SHIPMENT_STATE", 101L,
                "Cannot dispatch shipment #101: Current shipment status is CANCELLED.");
    }

    /**
     * Demonstrates unexpected system runtime exception -> HTTP 500 (safe generic payload, zero stack traces).
     * GET /api/exceptions/system-error
     */
    @GET
    @Path("/system-error")
    public Response testSystemError() {
        LOGGER.log(Level.INFO, "[ExceptionVerificationResource] Triggering controlled synthetic system error");
        throw new RuntimeException("Simulated unexpected database connection failure on cluster node-02.");
    }
}
