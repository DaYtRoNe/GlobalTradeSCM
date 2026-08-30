package com.jiat.globaltrade.security;

import com.jiat.globaltrade.security.dto.CallerSecuritySummary;
import com.jiat.globaltrade.service.AuditServiceBean;
import jakarta.annotation.Resource;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import java.io.Serializable;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dedicated Security Verification EJB demonstrating Jakarta EE 10 Role-Based Access Control (RBAC):
 * - Declarative Security: @DeclareRoles, @RolesAllowed, @PermitAll, @DenyAll
 * - Programmatic Security: SessionContext.getCallerPrincipal(), SessionContext.isCallerInRole(...)
 * - Autonomous Security Audit: AuditServiceBean (REQUIRES_NEW)
 */
@Stateless
@DeclareRoles({
        SecurityRoles.ADMIN,
        SecurityRoles.LOGISTICS_COORDINATOR,
        SecurityRoles.CUSTOMS_AGENT,
        SecurityRoles.WAREHOUSE_MANAGER,
        SecurityRoles.VENDOR_REPRESENTATIVE,
        SecurityRoles.CUSTOMER,
        SecurityRoles.SYSTEM
})
public class SecurityVerificationServiceBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(SecurityVerificationServiceBean.class.getName());

    @Resource
    private SessionContext sessionContext;

    @EJB
    private AuditServiceBean auditService;

    /**
     * Inspects caller principal and evaluates membership across all defined supply-chain roles.
     * Demonstrates programmatic authorization using SessionContext.
     */
    @PermitAll
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public CallerSecuritySummary getCallerSecurityInfo() {
        Principal principal = sessionContext.getCallerPrincipal();
        String callerName = principal != null ? principal.getName() : "ANONYMOUS";
        boolean authenticated = principal != null && !callerName.equalsIgnoreCase("anonymous");

        Map<String, Boolean> roles = new LinkedHashMap<>();
        roles.put(SecurityRoles.ADMIN, sessionContext.isCallerInRole(SecurityRoles.ADMIN));
        roles.put(SecurityRoles.LOGISTICS_COORDINATOR, sessionContext.isCallerInRole(SecurityRoles.LOGISTICS_COORDINATOR));
        roles.put(SecurityRoles.CUSTOMS_AGENT, sessionContext.isCallerInRole(SecurityRoles.CUSTOMS_AGENT));
        roles.put(SecurityRoles.WAREHOUSE_MANAGER, sessionContext.isCallerInRole(SecurityRoles.WAREHOUSE_MANAGER));
        roles.put(SecurityRoles.VENDOR_REPRESENTATIVE, sessionContext.isCallerInRole(SecurityRoles.VENDOR_REPRESENTATIVE));
        roles.put(SecurityRoles.CUSTOMER, sessionContext.isCallerInRole(SecurityRoles.CUSTOMER));
        roles.put(SecurityRoles.SYSTEM, sessionContext.isCallerInRole(SecurityRoles.SYSTEM));

        LOGGER.log(Level.INFO, "[SecurityVerificationServiceBean] Evaluated caller principal: {0}, Authenticated: {1}",
                new Object[]{callerName, authenticated});

        if (auditService != null && authenticated) {
            auditService.logAction("SECURITY_PRINCIPAL_INSPECTED", "SecurityContext", null, callerName,
                    "Caller security context and roles evaluated programmatically via SessionContext");
        }

        return new CallerSecuritySummary(callerName, authenticated, roles, "HTTP-BASIC-CONTAINER");
    }

    /**
     * Demonstrates fine-grained programmatic authorization.
     * International customs reviews are allowed only for ADMIN or CUSTOMS_AGENT roles.
     */
    @PermitAll
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public boolean mayReviewInternationalCustomsData() {
        Principal principal = sessionContext.getCallerPrincipal();
        String callerName = principal != null ? principal.getName() : "ANONYMOUS";

        boolean isCustoms = sessionContext.isCallerInRole(SecurityRoles.CUSTOMS_AGENT);
        boolean isAdmin = sessionContext.isCallerInRole(SecurityRoles.ADMIN);
        boolean authorized = isAdmin || isCustoms;

        LOGGER.log(Level.INFO, "[SecurityVerificationServiceBean] Programmatic authorization for customs review: Caller={0}, Authorized={1}",
                new Object[]{callerName, authorized});

        if (auditService != null) {
            auditService.logAction("SECURITY_PROGRAMMATIC_AUTH", "CustomsReview", null, callerName,
                    String.format("Customs review permission check: Authorized=%s (isCustoms=%s, isAdmin=%s)",
                            authorized, isCustoms, isAdmin));
        }

        return authorized;
    }

    /**
     * Admin-only privileged enterprise operation.
     */
    @RolesAllowed(SecurityRoles.ADMIN)
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public String performAdminOperation() {
        String caller = sessionContext.getCallerPrincipal().getName();
        LOGGER.log(Level.INFO, "[SecurityVerificationServiceBean] ADMIN operation executed by caller: {0}", caller);

        if (auditService != null) {
            auditService.logAction("SECURITY_ADMIN_OPERATION", "SecurityVerification", null, caller,
                    "Privileged administrative operation executed under @RolesAllowed(ADMIN)");
        }

        return "SUCCESS: Administrative operation completed by caller: " + caller;
    }

    /**
     * Customs clearance operation allowed for ADMIN or CUSTOMS_AGENT.
     */
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.CUSTOMS_AGENT})
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public String performCustomsOperation() {
        String caller = sessionContext.getCallerPrincipal().getName();
        LOGGER.log(Level.INFO, "[SecurityVerificationServiceBean] Customs operation executed by caller: {0}", caller);

        if (auditService != null) {
            auditService.logAction("SECURITY_CUSTOMS_OPERATION", "SecurityVerification", null, caller,
                    "Regulatory customs operation executed under @RolesAllowed({ADMIN, CUSTOMS_AGENT})");
        }

        return "SUCCESS: Regulatory customs operation completed by caller: " + caller;
    }

    /**
     * Warehouse inventory operation allowed for ADMIN or WAREHOUSE_MANAGER.
     */
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.WAREHOUSE_MANAGER})
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public String performWarehouseOperation() {
        String caller = sessionContext.getCallerPrincipal().getName();
        LOGGER.log(Level.INFO, "[SecurityVerificationServiceBean] Warehouse operation executed by caller: {0}", caller);

        if (auditService != null) {
            auditService.logAction("SECURITY_WAREHOUSE_OPERATION", "SecurityVerification", null, caller,
                    "Warehouse inventory operation executed under @RolesAllowed({ADMIN, WAREHOUSE_MANAGER})");
        }

        return "SUCCESS: Warehouse inventory operation completed by caller: " + caller;
    }

    /**
     * Logistics coordination operation allowed for ADMIN or LOGISTICS_COORDINATOR.
     */
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.LOGISTICS_COORDINATOR})
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public String performCoordinatorOperation() {
        String caller = sessionContext.getCallerPrincipal().getName();
        LOGGER.log(Level.INFO, "[SecurityVerificationServiceBean] Logistics coordination operation executed by caller: {0}", caller);

        if (auditService != null) {
            auditService.logAction("SECURITY_COORDINATOR_OPERATION", "SecurityVerification", null, caller,
                    "Logistics coordination operation executed under @RolesAllowed({ADMIN, LOGISTICS_COORDINATOR})");
        }

        return "SUCCESS: Logistics coordination operation completed by caller: " + caller;
    }

    /**
     * Vendor catalog operation allowed for ADMIN or VENDOR_REPRESENTATIVE.
     */
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.VENDOR_REPRESENTATIVE})
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public String performVendorOperation() {
        String caller = sessionContext.getCallerPrincipal().getName();
        LOGGER.log(Level.INFO, "[SecurityVerificationServiceBean] Vendor catalog operation executed by caller: {0}", caller);

        if (auditService != null) {
            auditService.logAction("SECURITY_VENDOR_OPERATION", "SecurityVerification", null, caller,
                    "Vendor catalog operation executed under @RolesAllowed({ADMIN, VENDOR_REPRESENTATIVE})");
        }

        return "SUCCESS: Vendor portal operation completed by caller: " + caller;
    }

    /**
     * Publicly accessible information endpoint.
     */
    @PermitAll
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public String getPublicInformation() {
        Principal principal = sessionContext.getCallerPrincipal();
        String caller = principal != null ? principal.getName() : "ANONYMOUS";
        return "SUCCESS: Public supply chain information accessed by caller: " + caller;
    }

    /**
     * Strictly restricted internal operation (Demonstrates @DenyAll).
     */
    @DenyAll
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public String performRestrictedInternalOperation() {
        return "RESTRICTED: This method is guarded by @DenyAll and must never execute.";
    }
}
