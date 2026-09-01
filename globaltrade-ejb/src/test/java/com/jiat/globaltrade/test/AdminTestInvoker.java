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
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;

/**
 * Test-only helper EJB executing business methods and test fixtures with the ADMIN security identity.
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

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

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

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void invokeRunnable(ThrowingRunnable runnable) throws Exception {
        runnable.run();
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void persist(Object entity) {
        em.persist(entity);
        em.flush();
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public <T> T merge(T entity) {
        T merged = em.merge(entity);
        em.flush();
        return merged;
    }

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }
}
