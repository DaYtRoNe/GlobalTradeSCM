package com.jiat.globaltrade.service;

import com.jiat.globaltrade.entity.Vendor;
import com.jiat.globaltrade.entity.enums.VendorStatus;
import com.jiat.globaltrade.interceptor.BusinessAuditInterceptor;
import com.jiat.globaltrade.interceptor.BusinessValidationInterceptor;
import com.jiat.globaltrade.interceptor.PerformanceMonitoringInterceptor;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Core business service for vendor management using Container-Managed Transactions (CMT).
 * Demonstrates Class-Level Interceptor Chaining:
 * 1. BusinessValidationInterceptor (Input constraints validation)
 * 2. PerformanceMonitoringInterceptor (Execution timing via System.nanoTime)
 * 3. BusinessAuditInterceptor (Autonomous audit logging via REQUIRES_NEW)
 */
@Stateless
@TransactionManagement(TransactionManagementType.CONTAINER)
@Interceptors({
        BusinessValidationInterceptor.class,
        PerformanceMonitoringInterceptor.class,
        BusinessAuditInterceptor.class
})
public class VendorServiceBean {

    private static final Logger LOGGER = Logger.getLogger(VendorServiceBean.class.getName());

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @EJB
    private AuditServiceBean auditService;

    /**
     * Read-only lookup for a single vendor by ID.
     * SUPPORTS allows calling within an existing transaction or running without one.
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Vendor findVendorById(Long id) {
        if (id == null) {
            return null;
        }
        return em.find(Vendor.class, id);
    }

    /**
     * Read-only lookup for all vendors.
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Vendor> findAllVendors() {
        return em.createQuery("SELECT v FROM Vendor v ORDER BY v.companyName ASC", Vendor.class)
                .getResultList();
    }

    /**
     * Updates the operational status of a vendor.
     * REQUIRED ensures this operation runs inside an active transaction.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Vendor updateVendorStatus(Long vendorId, VendorStatus newStatus, String performedBy) {
        if (vendorId == null || newStatus == null) {
            throw new IllegalArgumentException("Vendor ID and status must not be null.");
        }

        Vendor vendor = em.find(Vendor.class, vendorId);
        if (vendor == null) {
            LOGGER.log(Level.WARNING, "[VendorServiceBean] Vendor not found for ID: {0}", vendorId);
            return null;
        }

        VendorStatus oldStatus = vendor.getStatus();
        vendor.setStatus(newStatus);
        em.merge(vendor);

        LOGGER.log(Level.INFO, "[VendorServiceBean] [REQUIRED] Vendor {0} status updated from {1} to {2}",
                new Object[]{vendorId, oldStatus, newStatus});

        auditService.logAction("UPDATE_VENDOR_STATUS", "Vendor", vendorId, performedBy,
                String.format("Status changed from %s to %s", oldStatus, newStatus));

        return vendor;
    }

    /**
     * Updates the performance rating of a vendor.
     * REQUIRED ensures this update is persisted within a transaction.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Vendor updatePerformanceRating(Long vendorId, BigDecimal newRating, String performedBy) {
        if (vendorId == null || newRating == null) {
            throw new IllegalArgumentException("Vendor ID and rating must not be null.");
        }

        Vendor vendor = em.find(Vendor.class, vendorId);
        if (vendor == null) {
            LOGGER.log(Level.WARNING, "[VendorServiceBean] Vendor not found for ID: {0}", vendorId);
            return null;
        }

        BigDecimal oldRating = vendor.getPerformanceRating();
        vendor.setPerformanceRating(newRating);
        em.merge(vendor);

        LOGGER.log(Level.INFO, "[VendorServiceBean] [REQUIRED] Vendor {0} rating updated from {1} to {2}",
                new Object[]{vendorId, oldRating, newRating});

        auditService.logAction("UPDATE_VENDOR_RATING", "Vendor", vendorId, performedBy,
                String.format("Rating updated from %s to %s", oldRating, newRating));

        return vendor;
    }
}
