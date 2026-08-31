package com.jiat.globaltrade.web.resource;

import com.jiat.globaltrade.entity.Shipment;
import com.jiat.globaltrade.exception.ResourceNotFoundException;
import com.jiat.globaltrade.exception.ShipmentAccessDeniedException;
import com.jiat.globaltrade.security.SecurityRoles;
import com.jiat.globaltrade.service.ShipmentServiceBean;
import com.jiat.globaltrade.web.dto.ShipmentResponse;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Production REST Resource for Shipment operations across Customer and Staff Portals.
 * Base Path: /api/shipments
 */
@Stateless
@Path("/shipments")
@Produces(MediaType.APPLICATION_JSON)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@DeclareRoles({
        SecurityRoles.ADMIN,
        SecurityRoles.LOGISTICS_COORDINATOR,
        SecurityRoles.CUSTOMS_AGENT,
        SecurityRoles.WAREHOUSE_MANAGER,
        SecurityRoles.CUSTOMER
})
public class ShipmentResource {

    @EJB
    private ShipmentServiceBean shipmentService;

    /**
     * Lists all enterprise shipments for authorized operational staff.
     * GET /api/shipments
     */
    @GET
    @RolesAllowed({
            SecurityRoles.ADMIN,
            SecurityRoles.LOGISTICS_COORDINATOR,
            SecurityRoles.CUSTOMS_AGENT,
            SecurityRoles.WAREHOUSE_MANAGER
    })
    public Response getAllShipments() {
        List<Shipment> shipments = shipmentService.findAllShipments();
        List<ShipmentResponse> response = shipments.stream()
                .map(ShipmentResponse::fromEntity)
                .collect(Collectors.toList());
        return Response.ok(response).build();
    }

    /**
     * Customer self-service endpoint: lists only shipments assigned to the authenticated caller.
     * Identity is derived from SessionContext caller principal.
     * GET /api/shipments/my-shipments
     */
    @GET
    @Path("/my-shipments")
    @RolesAllowed(SecurityRoles.CUSTOMER)
    public Response getMyShipments() {
        List<Shipment> shipments = shipmentService.findMyShipments();
        List<ShipmentResponse> response = shipments.stream()
                .map(ShipmentResponse::fromEntity)
                .collect(Collectors.toList());
        return Response.ok(response).build();
    }

    /**
     * Returns detailed shipment information.
     * Accessible by operational staff or the customer owning the consignment.
     * GET /api/shipments/{id}
     */
    @GET
    @Path("/{id}")
    @RolesAllowed({
            SecurityRoles.ADMIN,
            SecurityRoles.LOGISTICS_COORDINATOR,
            SecurityRoles.CUSTOMS_AGENT,
            SecurityRoles.WAREHOUSE_MANAGER,
            SecurityRoles.CUSTOMER
    })
    public Response getShipmentById(@PathParam("id") Long id)
            throws ResourceNotFoundException, ShipmentAccessDeniedException {
        Shipment shipment = shipmentService.findShipmentForAuthorizedCaller(id);
        return Response.ok(ShipmentResponse.fromEntity(shipment)).build();
    }
}
