package com.jiat.globaltrade.web.resource;

import com.jiat.globaltrade.entity.AuditLog;
import com.jiat.globaltrade.security.SecurityRoles;
import com.jiat.globaltrade.service.AuditServiceBean;
import com.jiat.globaltrade.web.dto.AuditLogResponse;
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
 * Production REST Resource for Audit Logs in the Admin Portal.
 * Base Path: /api/audit-logs
 */
@Stateless
@Path("/audit-logs")
@Produces(MediaType.APPLICATION_JSON)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@DeclareRoles(SecurityRoles.ADMIN)
public class AuditResource {

    private static final int MAX_LIMIT = 100;
    private static final int DEFAULT_LIMIT = 50;

    @EJB
    private AuditServiceBean auditService;

    /**
     * Retrieves recent system-wide audit log entries for enterprise compliance oversight.
     * Restricted strictly to the ADMIN role. Enforces safe upper bound (max 100).
     * GET /api/audit-logs?limit=50
     */
    @GET
    @RolesAllowed(SecurityRoles.ADMIN)
    public Response getRecentAuditLogs(@QueryParam("limit") @DefaultValue("50") int limit) {
        int effectiveLimit = limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        List<AuditLog> logs = auditService.getRecentLogs(effectiveLimit);
        List<AuditLogResponse> response = logs.stream()
                .map(AuditLogResponse::fromEntity)
                .collect(Collectors.toList());
        return Response.ok(response).build();
    }
}
