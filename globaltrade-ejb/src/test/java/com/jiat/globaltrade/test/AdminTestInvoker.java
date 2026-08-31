package com.jiat.globaltrade.test;

import com.jiat.globaltrade.entity.Shipment;
import com.jiat.globaltrade.entity.Vendor;
import com.jiat.globaltrade.exception.InsufficientInventoryException;
import com.jiat.globaltrade.security.SecurityRoles;
import com.jiat.globaltrade.service.ShipmentServiceBean;
import com.jiat.globaltrade.service.VendorServiceBean;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RunAs;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import java.math.BigDecimal;

/**
 * Test-only helper EJB executing business methods with the ADMIN security identity.
 * This allows integration tests running within the container to invoke secured
 * business EJBs annotated with @RolesAllowed({ADMIN, ...}) without modifying or
 * weakening production security configurations.
 */
@Stateless
@PermitAll
@RunAs(SecurityRoles.ADMIN)
@DeclareRoles({
        SecurityRoles.ADMIN,
        SecurityRoles.LOGISTICS_COORDINATOR,
        SecurityRoles.WAREHOUSE_MANAGER
})
public class AdminTestInvoker {

    @EJB
    private ShipmentServiceBean shipmentService;

    @EJB
    private VendorServiceBean vendorService;

    public Shipment processShipmentDispatch(Long shipmentId, Long inventoryItemId, int quantity, String performedBy)
            throws InsufficientInventoryException {
        return shipmentService.processShipmentDispatch(shipmentId, inventoryItemId, quantity, performedBy);
    }

    public Vendor updatePerformanceRating(Long vendorId, BigDecimal rating, String performedBy) {
        return vendorService.updatePerformanceRating(vendorId, rating, performedBy);
    }
}
