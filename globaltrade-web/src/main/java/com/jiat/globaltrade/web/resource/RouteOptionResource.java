package com.jiat.globaltrade.web.resource;

import com.jiat.globaltrade.entity.RouteOption;
import com.jiat.globaltrade.security.SecurityRoles;
import com.jiat.globaltrade.service.RouteOptimizationServiceBean;
import com.jiat.globaltrade.web.dto.RouteOptionResponse;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Production REST Resource for Candidate Transport Route Options.
 * Base Path: /api/route-options
 */
@Stateless
@Path("/route-options")
@Produces(MediaType.APPLICATION_JSON)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@DeclareRoles({
        SecurityRoles.ADMIN,
        SecurityRoles.LOGISTICS_COORDINATOR
})
public class RouteOptionResource {

    @EJB
    private RouteOptimizationServiceBean routeOptimizationService;

    /**
     * Lists candidate route options with optional origin/destination/active filtering.
     * GET /api/route-options?origin=Tokyo&destination=Singapore&activeOnly=true
     */
    @GET
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.LOGISTICS_COORDINATOR})
    public Response getRouteOptions(
            @QueryParam("origin") String origin,
            @QueryParam("destination") String destination,
            @QueryParam("activeOnly") @DefaultValue("false") boolean activeOnly) {

        List<RouteOption> routes = routeOptimizationService.findRouteOptions(origin, destination, activeOnly);
        List<RouteOptionResponse> response = routes.stream()
                .map(RouteOptionResponse::fromEntity)
                .collect(Collectors.toList());

        return Response.ok(response).build();
    }
}
