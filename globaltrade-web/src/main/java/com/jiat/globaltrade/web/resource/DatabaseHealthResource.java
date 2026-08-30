package com.jiat.globaltrade.web.resource;

import com.jiat.globaltrade.service.SupplyChainDataService;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@Path("/health/database")
@Produces(MediaType.APPLICATION_JSON)
public class DatabaseHealthResource {

    private static final Logger LOGGER = Logger.getLogger(DatabaseHealthResource.class.getName());

    @EJB
    private SupplyChainDataService supplyChainDataService;

    @GET
    public Response checkDatabaseHealth() {
        boolean isConnected = false;
        long vendorCount = 0;
        String status = "DOWN";

        try {
            if (supplyChainDataService == null) {
                LOGGER.log(Level.SEVERE, "[DatabaseHealthResource] SupplyChainDataService @EJB injection failed (service reference is null).");
            } else {
                isConnected = supplyChainDataService.isDatabaseConnected();
                if (isConnected) {
                    vendorCount = supplyChainDataService.getVendorCount();
                    status = "UP";
                } else {
                    LOGGER.log(Level.WARNING, "[DatabaseHealthResource] SupplyChainDataService reported isDatabaseConnected() = false.");
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[DatabaseHealthResource] Unexpected exception during database health check: " + e.getMessage(), e);
            isConnected = false;
            status = "DOWN";
        }

        JsonObject responseJson = Json.createObjectBuilder()
                .add("databaseConnected", isConnected)
                .add("vendorCount", vendorCount)
                .add("status", status)
                .build();

        return Response.ok(responseJson).build();
    }
}


