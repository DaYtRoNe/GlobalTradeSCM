package com.jiat.globaltrade.web.resource;

import com.jiat.globaltrade.integration.model.CarrierTrackingPayload;
import com.jiat.globaltrade.integration.model.CustomsEdiPayload;
import com.jiat.globaltrade.integration.model.SupplierCatalogPayload;
import com.jiat.globaltrade.integration.model.WarehouseStockPayload;
import com.jiat.globaltrade.integration.service.IntegrationOrchestratorBean;
import com.jiat.globaltrade.security.SecurityRoles;
import com.jiat.globaltrade.web.dto.CarrierTrackingResponse;
import com.jiat.globaltrade.web.dto.CustomsEdiResponse;
import com.jiat.globaltrade.web.dto.IntegrationStatusResponse;
import com.jiat.globaltrade.web.dto.SupplierOrderResponse;
import com.jiat.globaltrade.web.dto.WarehouseStockResponse;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/integrations")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@DeclareRoles({
        SecurityRoles.ADMIN,
        SecurityRoles.LOGISTICS_COORDINATOR,
        SecurityRoles.CUSTOMS_AGENT,
        SecurityRoles.WAREHOUSE_MANAGER,
        SecurityRoles.VENDOR_REPRESENTATIVE,
        SecurityRoles.CUSTOMER
})
public class IntegrationResource {

    @EJB
    private IntegrationOrchestratorBean integrationOrchestrator;

    @GET
    @Path("/status")
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.LOGISTICS_COORDINATOR})
    public Response getIntegrationStatus() {
        IntegrationOrchestratorBean.IntegrationSystemStatusSummary summary =
                integrationOrchestrator.getIntegrationSystemStatus();

        IntegrationStatusResponse response = new IntegrationStatusResponse(
                summary.overallStatus(),
                summary.adapterEnvironment(),
                summary.timestamp() != null ? summary.timestamp().toString() : null,
                summary.gatewayStatusMap(),
                summary.activeGateways(),
                summary.degradedGateways()
        );

        return Response.ok(response).build();
    }

    @GET
    @Path("/carrier/{trackingNumber}")
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.LOGISTICS_COORDINATOR, SecurityRoles.CUSTOMER})
    public Response getCarrierTracking(@PathParam("trackingNumber") String trackingNumber) {
        CarrierTrackingPayload payload = integrationOrchestrator.getCarrierTracking(trackingNumber);
        if (payload == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Carrier tracking not found for: " + trackingNumber + "\"}")
                    .build();
        }

        CarrierTrackingResponse response = new CarrierTrackingResponse(
                payload.trackingNumber(),
                payload.carrierName(),
                payload.carrierCode(),
                payload.transportMode(),
                payload.externalStatusCode(),
                payload.currentCheckpoint(),
                payload.estimatedDeliveryWindow(),
                payload.lastEventTimestamp() != null ? payload.lastEventTimestamp().toString() : null,
                payload.integrationMode(),
                payload.sourceSystem()
        );

        return Response.ok(response).build();
    }

    @GET
    @Path("/customs/{documentNumber}")
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.LOGISTICS_COORDINATOR, SecurityRoles.CUSTOMS_AGENT})
    public Response getCustomsClearance(@PathParam("documentNumber") String documentNumber) {
        CustomsEdiPayload payload = integrationOrchestrator.getCustomsClearance(documentNumber);
        if (payload == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Customs clearance not found for: " + documentNumber + "\"}")
                    .build();
        }

        CustomsEdiResponse response = new CustomsEdiResponse(
                payload.documentNumber(),
                payload.declarationType(),
                payload.customsAuthority(),
                payload.clearanceStatusCode(),
                payload.entryNumber(),
                payload.dutyAssessedUsd(),
                payload.clearanceTimestamp() != null ? payload.clearanceTimestamp().toString() : null,
                payload.integrationMode(),
                payload.sourceSystem()
        );

        return Response.ok(response).build();
    }

    @GET
    @Path("/warehouse/{sku}")
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.LOGISTICS_COORDINATOR, SecurityRoles.WAREHOUSE_MANAGER})
    public Response getWarehouseStock(@PathParam("sku") String sku) {
        WarehouseStockPayload payload = integrationOrchestrator.getWarehouseStock(sku);
        if (payload == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Warehouse stock not found for SKU: " + sku + "\"}")
                    .build();
        }

        WarehouseStockResponse response = new WarehouseStockResponse(
                payload.sku(),
                payload.warehouseCode(),
                payload.binLocation(),
                payload.physicalOnHand(),
                payload.allocatedQuantity(),
                payload.availableToPromise(),
                payload.replenishmentStatus(),
                payload.syncTimestamp() != null ? payload.syncTimestamp().toString() : null,
                payload.integrationMode(),
                payload.sourceSystem()
        );

        return Response.ok(response).build();
    }

    @GET
    @Path("/supplier/{vendorCode}")
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.LOGISTICS_COORDINATOR, SecurityRoles.VENDOR_REPRESENTATIVE})
    public Response getSupplierPortalInfo(@PathParam("vendorCode") String vendorCode) {
        SupplierCatalogPayload payload = integrationOrchestrator.getSupplierPortalInfo(vendorCode);
        if (payload == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Supplier info not found for vendor: " + vendorCode + "\"}")
                    .build();
        }

        SupplierOrderResponse response = new SupplierOrderResponse(
                payload.vendorCode(),
                payload.companyName(),
                payload.supplierStatus(),
                payload.leadTimeDays(),
                payload.minimumOrderValueUsd(),
                payload.acceptElectronicPurchaseOrders(),
                payload.lastCatalogUpdate() != null ? payload.lastCatalogUpdate().toString() : null,
                payload.integrationMode(),
                payload.sourceSystem()
        );

        return Response.ok(response).build();
    }
}
