package com.jiat.globaltrade.web.resource;

import com.jiat.globaltrade.security.SecurityRoles;
import com.jiat.globaltrade.web.dto.ApiErrorResponse;
import com.jiat.globaltrade.web.dto.AuthResponse;
import com.jiat.globaltrade.web.dto.LoginRequest;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dedicated UI Authentication Endpoint.
 * Authenticates users programmatically through Payara's configured Security Realm
 * (GlobalTradeCustomRealm -> GlobalTradeLoginModule) via HttpServletRequest.login().
 * Crucially returns plain JSON 401 without WWW-Authenticate Basic challenge headers,
 * preventing browser-native Basic Auth popups.
 */
@Path("/ui-auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UiAuthResource {

    private static final Logger LOGGER = Logger.getLogger(UiAuthResource.class.getName());

    /**
     * Programmatically authenticates user against the container realm.
     * POST /api/ui-auth/login
     */
    @POST
    @Path("/login")
    public Response login(LoginRequest request, @Context HttpServletRequest httpRequest) {
        if (request == null || request.getUsername() == null || request.getPassword() == null ||
                request.getUsername().trim().isEmpty()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new ApiErrorResponse("UNAUTHORIZED", "Username and password are required."))
                    .build();
        }

        String username = request.getUsername().trim();

        try {
            // Logout any prior stale session on the connection before logging in
            try {
                if (httpRequest.getUserPrincipal() != null) {
                    httpRequest.logout();
                }
            } catch (Exception ignored) {
            }

            // Authenticate through Payara Web Container Realm
            httpRequest.login(username, request.getPassword());

            Principal principal = httpRequest.getUserPrincipal();
            String principalName = principal != null ? principal.getName() : username;

            Map<String, Boolean> roles = new LinkedHashMap<>();
            roles.put(SecurityRoles.ADMIN, httpRequest.isUserInRole(SecurityRoles.ADMIN));
            roles.put(SecurityRoles.LOGISTICS_COORDINATOR, httpRequest.isUserInRole(SecurityRoles.LOGISTICS_COORDINATOR));
            roles.put(SecurityRoles.CUSTOMS_AGENT, httpRequest.isUserInRole(SecurityRoles.CUSTOMS_AGENT));
            roles.put(SecurityRoles.WAREHOUSE_MANAGER, httpRequest.isUserInRole(SecurityRoles.WAREHOUSE_MANAGER));
            roles.put(SecurityRoles.VENDOR_REPRESENTATIVE, httpRequest.isUserInRole(SecurityRoles.VENDOR_REPRESENTATIVE));
            roles.put(SecurityRoles.CUSTOMER, httpRequest.isUserInRole(SecurityRoles.CUSTOMER));

            LOGGER.log(Level.INFO, "[UiAuthResource] UI Login successful for user: {0}", principalName);
            return Response.ok(new AuthResponse(true, principalName, roles)).build();

        } catch (ServletException e) {
            LOGGER.log(Level.WARNING, "[UiAuthResource] UI Login failed for user: {0} - Reason: {1}",
                    new Object[]{username, e.getMessage()});
            // Return JSON 401 WITHOUT WWW-Authenticate header
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new ApiErrorResponse("UNAUTHORIZED", "Invalid username or password."))
                    .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[UiAuthResource] Unexpected error during UI login for user: " + username, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ApiErrorResponse("INTERNAL_SERVER_ERROR", "An internal error occurred during authentication."))
                    .build();
        }
    }
}
