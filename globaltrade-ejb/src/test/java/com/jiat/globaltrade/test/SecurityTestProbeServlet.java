package com.jiat.globaltrade.test;

import com.jiat.globaltrade.entity.Vendor;
import com.jiat.globaltrade.exception.ResourceNotFoundException;
import com.jiat.globaltrade.exception.VendorAccessDeniedException;
import com.jiat.globaltrade.security.SecurityRoles;
import com.jiat.globaltrade.security.VendorAuthorizationServiceBean;
import jakarta.ejb.EJB;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Test-only HTTP Servlet probe used by SecurityAuthenticationIT to exercise
 * real Payara container authentication (GlobalTradeCustomRealm), declarative web/EJB RBAC,
 * and fine-grained programmatic vendor authorization.
 */
@WebServlet(urlPatterns = {"/security-test/*"})
public class SecurityTestProbeServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(SecurityTestProbeServlet.class.getName());

    @EJB
    private VendorAuthorizationServiceBean vendorAuthService;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null) {
            pathInfo = "/";
        }

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();

        Principal principal = req.getUserPrincipal();
        String username = principal != null ? principal.getName() : "ANONYMOUS";

        if ("/whoami".equals(pathInfo)) {
            boolean isAdmin = req.isUserInRole(SecurityRoles.ADMIN);
            boolean isCoordinator = req.isUserInRole(SecurityRoles.LOGISTICS_COORDINATOR);
            boolean isCustoms = req.isUserInRole(SecurityRoles.CUSTOMS_AGENT);
            boolean isWarehouse = req.isUserInRole(SecurityRoles.WAREHOUSE_MANAGER);
            boolean isVendor = req.isUserInRole(SecurityRoles.VENDOR_REPRESENTATIVE);
            boolean isCustomer = req.isUserInRole(SecurityRoles.CUSTOMER);

            out.write(String.format(
                    "{\"status\":\"SUCCESS\",\"authenticated\":%b,\"principal\":\"%s\"," +
                    "\"roles\":{\"ADMIN\":%b,\"LOGISTICS_COORDINATOR\":%b,\"CUSTOMS_AGENT\":%b," +
                    "\"WAREHOUSE_MANAGER\":%b,\"VENDOR_REPRESENTATIVE\":%b,\"CUSTOMER\":%b}}",
                    principal != null, username, isAdmin, isCoordinator, isCustoms, isWarehouse, isVendor, isCustomer
            ));
            return;
        }

        if ("/admin".equals(pathInfo)) {
            if (!req.isUserInRole(SecurityRoles.ADMIN)) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.write("{\"status\":\"FORBIDDEN\",\"message\":\"Caller is not in role ADMIN\"}");
                return;
            }
            out.write(String.format("{\"status\":\"SUCCESS\",\"message\":\"Admin clearance confirmed\",\"principal\":\"%s\"}", username));
            return;
        }

        if ("/customs".equals(pathInfo)) {
            if (!req.isUserInRole(SecurityRoles.CUSTOMS_AGENT) && !req.isUserInRole(SecurityRoles.ADMIN)) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.write("{\"status\":\"FORBIDDEN\",\"message\":\"Caller is not in role CUSTOMS_AGENT\"}");
                return;
            }
            out.write(String.format("{\"status\":\"SUCCESS\",\"message\":\"Customs clearance confirmed\",\"principal\":\"%s\"}", username));
            return;
        }

        if (pathInfo.startsWith("/vendor/")) {
            String idStr = pathInfo.substring("/vendor/".length());
            try {
                Long vendorId = Long.parseLong(idStr);
                Vendor vendor = vendorAuthService.getVendorForAuthorizedCaller(vendorId);
                out.write(String.format(
                        "{\"status\":\"SUCCESS\",\"vendorId\":%d,\"vendorCode\":\"%s\",\"companyName\":\"%s\",\"caller\":\"%s\"}",
                        vendor.getId(), vendor.getVendorCode(), vendor.getCompanyName(), username
                ));
            } catch (VendorAccessDeniedException e) {
                LOGGER.log(Level.WARNING, "[SecurityTestProbeServlet] Access denied to vendor #{0} for caller: {1}",
                        new Object[]{idStr, username});
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.write(String.format("{\"status\":\"FORBIDDEN\",\"message\":\"%s\"}", e.getMessage()));
            } catch (ResourceNotFoundException e) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.write(String.format("{\"status\":\"NOT_FOUND\",\"message\":\"%s\"}", e.getMessage()));
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "[SecurityTestProbeServlet] Error invoking vendor authorization service", e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.write(String.format("{\"status\":\"ERROR\",\"message\":\"%s\"}", e.getMessage()));
            }
            return;
        }

        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        out.write("{\"status\":\"NOT_FOUND\",\"message\":\"Unknown probe endpoint\"}");
    }
}
