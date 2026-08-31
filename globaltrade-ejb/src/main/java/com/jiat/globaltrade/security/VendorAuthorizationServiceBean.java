package com.jiat.globaltrade.security;

import com.jiat.globaltrade.entity.Vendor;
import com.jiat.globaltrade.exception.ResourceNotFoundException;
import com.jiat.globaltrade.exception.VendorAccessDeniedException;
import com.jiat.globaltrade.service.AuditServiceBean;
import jakarta.annotation.Resource;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.Serializable;
import java.security.Principal;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dedicated Security Authorization EJB enforcing fine-grained vendor data access controls.
 * Combines Declarative RBAC (@RolesAllowed) with Programmatic Authorization (SessionContext & vendor_user_access).
 */
@Stateless
@DeclareRoles({
        SecurityRoles.ADMIN,
        SecurityRoles.LOGISTICS_COORDINATOR,
        SecurityRoles.VENDOR_REPRESENTATIVE,
        SecurityRoles.CUSTOMS_AGENT,
        SecurityRoles.WAREHOUSE_MANAGER,
        SecurityRoles.CUSTOMER,
        SecurityRoles.SYSTEM
})
public class VendorAuthorizationServiceBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(VendorAuthorizationServiceBean.class.getName());

    @Resource
    private SessionContext sessionContext;

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @EJB
    private AuditServiceBean auditService;

    /**
     * Programmatically verifies if the authenticated caller has permission to view/modify the specified vendor.
     * Authorization Rules:
     * - ADMIN: Enterprise-wide access to all vendors.
     * - LOGISTICS_COORDINATOR: Operational access to all vendors.
     * - VENDOR_REPRESENTATIVE: Strictly restricted to the vendor ID mapped in vendor_user_access.
     * - Other roles: Denied.
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public boolean isCallerAuthorizedForVendor(Long vendorId) {
        if (vendorId == null || vendorId <= 0) {
            return false;
        }

        Principal principal = sessionContext.getCallerPrincipal();
        String username = principal != null ? principal.getName() : "ANONYMOUS";

        if (sessionContext.isCallerInRole(SecurityRoles.ADMIN)) {
            LOGGER.log(Level.INFO, "[VendorAuthorizationServiceBean] Access GRANTED to Vendor #{0} for ADMIN: {1}",
                    new Object[]{vendorId, username});
            logSecurityAudit("VENDOR_ACCESS_GRANTED", vendorId, username, "Authorized via ADMIN role");
            return true;
        }

        if (sessionContext.isCallerInRole(SecurityRoles.LOGISTICS_COORDINATOR)) {
            LOGGER.log(Level.INFO, "[VendorAuthorizationServiceBean] Access GRANTED to Vendor #{0} for LOGISTICS_COORDINATOR: {1}",
                    new Object[]{vendorId, username});
            logSecurityAudit("VENDOR_ACCESS_GRANTED", vendorId, username, "Authorized via LOGISTICS_COORDINATOR role");
            return true;
        }

        if (sessionContext.isCallerInRole(SecurityRoles.VENDOR_REPRESENTATIVE)) {
            // Fine-grained database mapping lookup in vendor_user_access
            Number count = (Number) em.createNativeQuery(
                    "SELECT COUNT(*) FROM vendor_user_access WHERE username = ?1 AND vendor_id = ?2")
                    .setParameter(1, username)
                    .setParameter(2, vendorId)
                    .getSingleResult();

            boolean hasAccess = count != null && count.longValue() > 0;
            if (hasAccess) {
                LOGGER.log(Level.INFO, "[VendorAuthorizationServiceBean] Fine-grained access GRANTED to Vendor #{0} for VENDOR_REPRESENTATIVE: {1}",
                        new Object[]{vendorId, username});
                logSecurityAudit("VENDOR_ACCESS_GRANTED", vendorId, username, "Authorized via fine-grained vendor mapping");
                return true;
            } else {
                LOGGER.log(Level.WARNING, "[VendorAuthorizationServiceBean] Cross-vendor access DENIED to Vendor #{0} for VENDOR_REPRESENTATIVE: {1}",
                        new Object[]{vendorId, username});
                logSecurityAudit("VENDOR_ACCESS_DENIED", vendorId, username, "Cross-vendor access violation attempt");
                return false;
            }
        }

        LOGGER.log(Level.WARNING, "[VendorAuthorizationServiceBean] Access DENIED to Vendor #{0} for unauthorized role/user: {1}",
                new Object[]{vendorId, username});
        logSecurityAudit("VENDOR_ACCESS_DENIED", vendorId, username, "Caller lacks required vendor authorization");
        return false;
    }

    /**
     * Retrieves vendor entity for an authorized caller.
     * Enforces declarative role check followed by fine-grained programmatic vendor ownership check.
     */
    @RolesAllowed({
            SecurityRoles.ADMIN,
            SecurityRoles.LOGISTICS_COORDINATOR,
            SecurityRoles.VENDOR_REPRESENTATIVE
    })
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Vendor getVendorForAuthorizedCaller(Long vendorId) throws VendorAccessDeniedException, ResourceNotFoundException {
        String username = sessionContext.getCallerPrincipal() != null ? sessionContext.getCallerPrincipal().getName() : "ANONYMOUS";

        if (!isCallerAuthorizedForVendor(vendorId)) {
            throw new VendorAccessDeniedException(vendorId, username);
        }

        Vendor vendor = em.find(Vendor.class, vendorId);
        if (vendor == null) {
            LOGGER.log(Level.WARNING, "[VendorAuthorizationServiceBean] Vendor #{0} not found.", vendorId);
            throw new ResourceNotFoundException("Vendor", vendorId);
        }

        return vendor;
    }

    /**
     * Looks up the vendor entity mapped to the authenticated VENDOR_REPRESENTATIVE.
     * Allows vendor representative to query their own profile without providing a hardcoded ID.
     */
    @RolesAllowed(SecurityRoles.VENDOR_REPRESENTATIVE)
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Vendor findMappedVendorForCaller() throws VendorAccessDeniedException, ResourceNotFoundException {
        String username = sessionContext.getCallerPrincipal() != null ? sessionContext.getCallerPrincipal().getName() : "ANONYMOUS";

        List<?> results = em.createNativeQuery("SELECT vendor_id FROM vendor_user_access WHERE username = ?1")
                .setParameter(1, username)
                .getResultList();

        if (results == null || results.isEmpty()) {
            LOGGER.log(Level.WARNING, "[VendorAuthorizationServiceBean] No vendor mapping found for user: {0}", username);
            throw new VendorAccessDeniedException(0L, username);
        }

        Number vendorIdNum = (Number) results.get(0);
        Long vendorId = vendorIdNum.longValue();
        Vendor vendor = em.find(Vendor.class, vendorId);
        if (vendor == null) {
            throw new ResourceNotFoundException("Vendor", vendorId);
        }
        return vendor;
    }

    private void logSecurityAudit(String action, Long vendorId, String performedBy, String details) {
        if (auditService != null) {
            auditService.logAction(action, "Vendor", vendorId, performedBy, details);
        }
    }
}
