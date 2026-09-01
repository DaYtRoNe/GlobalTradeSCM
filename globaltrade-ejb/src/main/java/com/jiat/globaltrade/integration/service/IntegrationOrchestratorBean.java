package com.jiat.globaltrade.integration.service;

import com.jiat.globaltrade.integration.gateway.CustomsSystemGateway;
import com.jiat.globaltrade.integration.gateway.ShippingCarrierGateway;
import com.jiat.globaltrade.integration.gateway.SupplierPortalGateway;
import com.jiat.globaltrade.integration.gateway.WarehouseManagementGateway;
import com.jiat.globaltrade.integration.model.CarrierTrackingPayload;
import com.jiat.globaltrade.integration.model.CustomsEdiPayload;
import com.jiat.globaltrade.integration.model.SupplierCatalogPayload;
import com.jiat.globaltrade.integration.model.WarehouseStockPayload;
import com.jiat.globaltrade.security.SecurityRoles;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Enterprise Service Orchestrator for External Gateway Integrations.
 * Aggregates and coordinates communication across Shipping Carriers, Customs Authorities,
 * Warehouse Management Systems (WMS), and Vendor Supplier Portals.
 */
@Stateless
@TransactionManagement(TransactionManagementType.CONTAINER)
@DeclareRoles({
        SecurityRoles.ADMIN,
        SecurityRoles.LOGISTICS_COORDINATOR,
        SecurityRoles.CUSTOMS_AGENT,
        SecurityRoles.WAREHOUSE_MANAGER,
        SecurityRoles.VENDOR_REPRESENTATIVE,
        SecurityRoles.CUSTOMER,
        SecurityRoles.SYSTEM
})
public class IntegrationOrchestratorBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(IntegrationOrchestratorBean.class.getName());

    @EJB
    private ShippingCarrierGateway carrierGateway;

    @EJB
    private CustomsSystemGateway customsGateway;

    @EJB
    private WarehouseManagementGateway warehouseGateway;

    @EJB
    private SupplierPortalGateway supplierGateway;

    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.LOGISTICS_COORDINATOR, SecurityRoles.SYSTEM})
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public IntegrationSystemStatusSummary getIntegrationSystemStatus() {
        LOGGER.log(Level.INFO, "[IntegrationOrchestrator] Auditing connectivity to all 4 external partner gateways...");

        return new IntegrationSystemStatusSummary(
                "CONNECTED",
                "SIMULATED_ENTERPRISE_GATEWAYS",
                LocalDateTime.now(),
                Map.of(
                        "ShippingCarrierGateway", "OPERATIONAL (SIMULATED)",
                        "CustomsSystemGateway", "OPERATIONAL (SIMULATED)",
                        "WarehouseManagementGateway", "OPERATIONAL (SIMULATED)",
                        "SupplierPortalGateway", "OPERATIONAL (SIMULATED)"
                ),
                4,
                0
        );
    }

    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.LOGISTICS_COORDINATOR, SecurityRoles.CUSTOMER, SecurityRoles.SYSTEM})
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public CarrierTrackingPayload getCarrierTracking(String trackingNumber) {
        return carrierGateway.fetchCarrierTracking(trackingNumber);
    }

    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.LOGISTICS_COORDINATOR, SecurityRoles.CUSTOMS_AGENT, SecurityRoles.SYSTEM})
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public CustomsEdiPayload getCustomsClearance(String documentNumber) {
        return customsGateway.queryClearanceStatus(documentNumber);
    }

    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.LOGISTICS_COORDINATOR, SecurityRoles.WAREHOUSE_MANAGER, SecurityRoles.SYSTEM})
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public WarehouseStockPayload getWarehouseStock(String sku) {
        return warehouseGateway.queryBinStock(sku);
    }

    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.LOGISTICS_COORDINATOR, SecurityRoles.VENDOR_REPRESENTATIVE, SecurityRoles.SYSTEM})
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public SupplierCatalogPayload getSupplierPortalInfo(String vendorCode) {
        return supplierGateway.querySupplierInfo(vendorCode);
    }

    public record IntegrationSystemStatusSummary(
            String overallStatus,
            String adapterEnvironment,
            LocalDateTime timestamp,
            Map<String, String> gatewayStatusMap,
            int activeGateways,
            int degradedGateways
    ) implements Serializable {}
}
