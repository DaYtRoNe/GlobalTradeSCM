package com.jiat.globaltrade.web.resource;

import com.jiat.globaltrade.entity.InventoryItem;
import com.jiat.globaltrade.security.SecurityRoles;
import com.jiat.globaltrade.service.InventoryServiceBean;
import com.jiat.globaltrade.web.dto.InventoryItemResponse;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Production REST Resource for Inventory queries in Admin and Warehouse Portals.
 * Base Path: /api/inventory
 */
@Stateless
@Path("/inventory")
@Produces(MediaType.APPLICATION_JSON)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@DeclareRoles({
        SecurityRoles.ADMIN,
        SecurityRoles.WAREHOUSE_MANAGER,
        SecurityRoles.LOGISTICS_COORDINATOR
})
public class InventoryResource {

    @EJB
    private InventoryServiceBean inventoryService;

    /**
     * Lists all inventory items across all warehouse facilities.
     * Restricted to Admin, Warehouse Managers, and Logistics Coordinators.
     * GET /api/inventory
     */
    @GET
    @RolesAllowed({
            SecurityRoles.ADMIN,
            SecurityRoles.WAREHOUSE_MANAGER,
            SecurityRoles.LOGISTICS_COORDINATOR
    })
    public Response getAllInventoryItems() {
        List<InventoryItem> items = inventoryService.findAllInventoryItems();
        List<InventoryItemResponse> response = items.stream()
                .map(InventoryItemResponse::fromEntity)
                .collect(Collectors.toList());
        return Response.ok(response).build();
    }
}
