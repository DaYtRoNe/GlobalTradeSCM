package com.jiat.globaltrade.web.resource;

import com.jiat.globaltrade.entity.CustomsDocument;
import com.jiat.globaltrade.exception.ResourceNotFoundException;
import com.jiat.globaltrade.exception.ShipmentAccessDeniedException;
import com.jiat.globaltrade.security.SecurityRoles;
import com.jiat.globaltrade.service.CustomsServiceBean;
import com.jiat.globaltrade.web.dto.CustomsDocumentResponse;
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
 * Production REST Resource for Customs Declarations across Staff and Customer Portals.
 * Base Path: /api/customs
 */
@Stateless
@Path("/customs")
@Produces(MediaType.APPLICATION_JSON)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@DeclareRoles({
        SecurityRoles.ADMIN,
        SecurityRoles.CUSTOMS_AGENT,
        SecurityRoles.LOGISTICS_COORDINATOR,
        SecurityRoles.CUSTOMER
})
public class CustomsResource {

    @EJB
    private CustomsServiceBean customsService;

    /**
     * Lists all enterprise customs documents for customs officers and administrators.
     * GET /api/customs
     */
    @GET
    @RolesAllowed({
            SecurityRoles.ADMIN,
            SecurityRoles.CUSTOMS_AGENT,
            SecurityRoles.LOGISTICS_COORDINATOR
    })
    public Response getAllCustomsDocuments() {
        List<CustomsDocument> docs = customsService.findAllCustomsDocuments();
        List<CustomsDocumentResponse> response = docs.stream()
                .map(CustomsDocumentResponse::fromEntity)
                .collect(Collectors.toList());
        return Response.ok(response).build();
    }

    /**
     * Lists customs documents linked to a specific shipment.
     * Accessible by operational staff or the customer who owns the shipment.
     * GET /api/customs/shipment/{shipmentId}
     */
    @GET
    @Path("/shipment/{shipmentId}")
    @RolesAllowed({
            SecurityRoles.ADMIN,
            SecurityRoles.CUSTOMS_AGENT,
            SecurityRoles.LOGISTICS_COORDINATOR,
            SecurityRoles.CUSTOMER
    })
    public Response getDocumentsByShipment(@PathParam("shipmentId") Long shipmentId)
            throws ResourceNotFoundException, ShipmentAccessDeniedException {
        List<CustomsDocument> docs = customsService.findDocumentsByShipmentForCaller(shipmentId);
        List<CustomsDocumentResponse> response = docs.stream()
                .map(CustomsDocumentResponse::fromEntity)
                .collect(Collectors.toList());
        return Response.ok(response).build();
    }
}
