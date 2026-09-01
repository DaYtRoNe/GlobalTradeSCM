package com.jiat.globaltrade.web.resource;

import com.jiat.globaltrade.entity.SupplyChainAlert;
import com.jiat.globaltrade.entity.enums.SupplyChainAlertStatus;
import com.jiat.globaltrade.exception.ResourceNotFoundException;
import com.jiat.globaltrade.security.SecurityRoles;
import com.jiat.globaltrade.service.SupplyChainAlertServiceBean;
import com.jiat.globaltrade.web.dto.SupplyChainAlertResponse;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Production REST Resource for Supply Chain Monitoring Alerts.
 * Base Path: /api/alerts
 * Provides role-scoped alert feeds and acknowledgement operations.
 */
@Stateless
@Path("/alerts")
@Produces(MediaType.APPLICATION_JSON)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@DeclareRoles({
        SecurityRoles.ADMIN,
        SecurityRoles.LOGISTICS_COORDINATOR,
        SecurityRoles.CUSTOMS_AGENT,
        SecurityRoles.WAREHOUSE_MANAGER,
        SecurityRoles.VENDOR_REPRESENTATIVE,
        SecurityRoles.CUSTOMER
})
public class SupplyChainAlertResource {

    @EJB
    private SupplyChainAlertServiceBean alertService;

    /**
     * Lists alerts visible to the authenticated caller with optional status filtering.
     * GET /api/alerts?status=OPEN
     */
    @GET
    @RolesAllowed({
            SecurityRoles.ADMIN,
            SecurityRoles.LOGISTICS_COORDINATOR,
            SecurityRoles.CUSTOMS_AGENT,
            SecurityRoles.WAREHOUSE_MANAGER,
            SecurityRoles.VENDOR_REPRESENTATIVE,
            SecurityRoles.CUSTOMER
    })
    public Response getAlerts(@QueryParam("status") SupplyChainAlertStatus status) {
        List<SupplyChainAlert> alerts = alertService.findAlertsForCaller(status);
        List<SupplyChainAlertResponse> response = alerts.stream()
                .map(SupplyChainAlertResponse::fromEntity)
                .collect(Collectors.toList());
        return Response.ok(response).build();
    }

    /**
     * Acknowledges an active alert.
     * POST /api/alerts/{id}/acknowledge
     */
    @POST
    @Path("/{id}/acknowledge")
    @RolesAllowed({
            SecurityRoles.ADMIN,
            SecurityRoles.LOGISTICS_COORDINATOR,
            SecurityRoles.CUSTOMS_AGENT,
            SecurityRoles.WAREHOUSE_MANAGER,
            SecurityRoles.VENDOR_REPRESENTATIVE,
            SecurityRoles.CUSTOMER
    })
    public Response acknowledgeAlert(@PathParam("id") Long id) throws ResourceNotFoundException {
        SupplyChainAlert alert = alertService.acknowledgeAlert(id);
        return Response.ok(SupplyChainAlertResponse.fromEntity(alert)).build();
    }
}
