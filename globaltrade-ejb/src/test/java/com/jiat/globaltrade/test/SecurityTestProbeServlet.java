package com.jiat.globaltrade.test;

import com.jiat.globaltrade.entity.AuditLog;
import com.jiat.globaltrade.entity.CustomsDocument;
import com.jiat.globaltrade.entity.InventoryItem;
import com.jiat.globaltrade.entity.Shipment;
import com.jiat.globaltrade.entity.Vendor;
import com.jiat.globaltrade.exception.ResourceNotFoundException;
import com.jiat.globaltrade.exception.ShipmentAccessDeniedException;
import com.jiat.globaltrade.exception.VendorAccessDeniedException;
import com.jiat.globaltrade.security.SecurityRoles;
import com.jiat.globaltrade.security.VendorAuthorizationServiceBean;
import com.jiat.globaltrade.service.AuditServiceBean;
import com.jiat.globaltrade.service.CustomsServiceBean;
import com.jiat.globaltrade.service.InventoryServiceBean;
import com.jiat.globaltrade.service.ShipmentServiceBean;
import com.jiat.globaltrade.service.VendorServiceBean;
import jakarta.ejb.EJB;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Test-only HTTP Servlet probe used by integration tests to exercise
 * container authentication, declarative RBAC, and fine-grained data authorization.
 */
@WebServlet(urlPatterns = {"/security-test/*"})
public class SecurityTestProbeServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(SecurityTestProbeServlet.class.getName());

    @EJB
    private VendorAuthorizationServiceBean vendorAuthService;

    @EJB
    private ShipmentServiceBean shipmentService;

    @EJB
    private CustomsServiceBean customsService;

    @EJB
    private VendorServiceBean vendorService;

    @EJB
    private InventoryServiceBean inventoryService;

    @EJB
    private AuditServiceBean auditService;

    @EJB
    private com.jiat.globaltrade.service.SupplyChainAlertServiceBean alertService;

    @EJB
    private com.jiat.globaltrade.service.SupplyChainMonitoringServiceBean monitoringService;

    @EJB
    private com.jiat.globaltrade.service.SupplyChainMonitoringWorkerBean workerBean;

    @EJB
    private com.jiat.globaltrade.service.RouteOptimizationServiceBean routeOptimizationService;

    @EJB
    private com.jiat.globaltrade.service.RouteOptimizationCoordinatorBean routeCoordinator;

    @EJB
    private AdminTestInvoker invoker;

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

        if ("/shipment/my-shipments".equals(pathInfo)) {
            try {
                List<Shipment> shipments = shipmentService.findMyShipments();
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < shipments.size(); i++) {
                    Shipment s = shipments.get(i);
                    sb.append(String.format("{\"id\":%d,\"trackingNumber\":\"%s\",\"customerUsername\":\"%s\"}",
                            s.getId(), s.getTrackingNumber(), s.getCustomerUsername()));
                    if (i < shipments.size() - 1) sb.append(",");
                }
                sb.append("]");
                out.write(sb.toString());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "[SecurityTestProbeServlet] Error in my-shipments for caller: " + username, e);
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.write(String.format("{\"status\":\"FORBIDDEN\",\"message\":\"%s\"}", e.getMessage()));
            }
            return;
        }

        if (pathInfo.startsWith("/shipment/")) {
            String idStr = pathInfo.substring("/shipment/".length());
            try {
                Long shipmentId = Long.parseLong(idStr);
                Shipment s = shipmentService.findShipmentForAuthorizedCaller(shipmentId);
                out.write(String.format(
                        "{\"status\":\"SUCCESS\",\"id\":%d,\"trackingNumber\":\"%s\",\"customerUsername\":\"%s\",\"caller\":\"%s\"}",
                        s.getId(), s.getTrackingNumber(), s.getCustomerUsername() != null ? s.getCustomerUsername() : "null", username
                ));
            } catch (ShipmentAccessDeniedException e) {
                LOGGER.log(Level.WARNING, "[SecurityTestProbeServlet] Access denied to shipment #{0} for caller: {1}",
                        new Object[]{idStr, username});
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.write(String.format("{\"status\":\"FORBIDDEN\",\"message\":\"%s\"}", e.getMessage()));
            } catch (ResourceNotFoundException e) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.write(String.format("{\"status\":\"NOT_FOUND\",\"message\":\"%s\"}", e.getMessage()));
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "[SecurityTestProbeServlet] Error invoking shipment authorization", e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.write(String.format("{\"status\":\"ERROR\",\"message\":\"%s\"}", e.getMessage()));
            }
            return;
        }

        if (pathInfo.startsWith("/customs-docs/")) {
            String idStr = pathInfo.substring("/customs-docs/".length());
            try {
                Long shipmentId = Long.parseLong(idStr);
                List<CustomsDocument> docs = customsService.findDocumentsByShipmentForCaller(shipmentId);
                out.write(String.format("{\"status\":\"SUCCESS\",\"shipmentId\":%d,\"docCount\":%d}", shipmentId, docs.size()));
            } catch (ShipmentAccessDeniedException e) {
                LOGGER.log(Level.WARNING, "[SecurityTestProbeServlet] Access denied to customs for shipment #{0} for caller: {1}",
                        new Object[]{idStr, username});
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.write(String.format("{\"status\":\"FORBIDDEN\",\"message\":\"%s\"}", e.getMessage()));
            } catch (ResourceNotFoundException e) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.write(String.format("{\"status\":\"NOT_FOUND\",\"message\":\"%s\"}", e.getMessage()));
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "[SecurityTestProbeServlet] Error invoking customs authorization", e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.write(String.format("{\"status\":\"ERROR\",\"message\":\"%s\"}", e.getMessage()));
            }
            return;
        }

        if ("/staff-data/vendors".equals(pathInfo)) {
            List<Vendor> list = vendorService.findAllVendors();
            out.write(String.format("{\"status\":\"SUCCESS\",\"count\":%d}", list.size()));
            return;
        }

        if ("/staff-data/inventory".equals(pathInfo)) {
            List<InventoryItem> list = inventoryService.findAllInventoryItems();
            out.write(String.format("{\"status\":\"SUCCESS\",\"count\":%d}", list.size()));
            return;
        }

        if ("/staff-data/customs".equals(pathInfo)) {
            List<CustomsDocument> list = customsService.findAllCustomsDocuments();
            out.write(String.format("{\"status\":\"SUCCESS\",\"count\":%d}", list.size()));
            return;
        }

        if ("/staff-data/shipments".equals(pathInfo)) {
            List<Shipment> list = shipmentService.findAllShipments();
            out.write(String.format("{\"status\":\"SUCCESS\",\"count\":%d}", list.size()));
            return;
        }

        if ("/staff-data/audit-logs".equals(pathInfo)) {
            List<AuditLog> list = auditService.getRecentLogs(50);
            out.write(String.format("{\"status\":\"SUCCESS\",\"count\":%d}", list.size()));
            return;
        }

        if ("/alerts".equals(pathInfo)) {
            try {
                List<com.jiat.globaltrade.entity.SupplyChainAlert> alerts = alertService.findAlertsForCaller(null);
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < alerts.size(); i++) {
                    com.jiat.globaltrade.entity.SupplyChainAlert a = alerts.get(i);
                    sb.append(String.format(
                            "{\"id\":%d,\"alertKey\":\"%s\",\"alertType\":\"%s\",\"alertStatus\":\"%s\",\"entityType\":\"%s\",\"entityId\":%d,\"message\":\"%s\"}",
                            a.getId(), a.getAlertKey(), a.getAlertType(), a.getAlertStatus(), a.getEntityType(), a.getEntityId(), a.getMessage().replace("\"", "\\\"")
                    ));
                    if (i < alerts.size() - 1) sb.append(",");
                }
                sb.append("]");
                out.write(sb.toString());
            } catch (Exception e) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.write(String.format("{\"status\":\"FORBIDDEN\",\"message\":\"%s\"}", e.getMessage()));
            }
            return;
        }

        if ("/route-options".equals(pathInfo)) {
            if (!req.isUserInRole(SecurityRoles.ADMIN) && !req.isUserInRole(SecurityRoles.LOGISTICS_COORDINATOR)) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.write("{\"status\":\"FORBIDDEN\",\"message\":\"Access denied to route options\"}");
                return;
            }
            try {
                String origin = req.getParameter("origin");
                String dest = req.getParameter("destination");
                boolean activeOnly = "true".equalsIgnoreCase(req.getParameter("activeOnly"));
                List<com.jiat.globaltrade.entity.RouteOption> routes = routeOptimizationService.findRouteOptions(origin, dest, activeOnly);
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < routes.size(); i++) {
                    com.jiat.globaltrade.entity.RouteOption r = routes.get(i);
                    sb.append(String.format(
                            "{\"id\":%d,\"routeCode\":\"%s\",\"origin\":\"%s\",\"destination\":\"%s\",\"cost\":%s,\"hours\":%d,\"risk\":%s,\"active\":%b}",
                            r.getId(), r.getRouteCode(), r.getOrigin(), r.getDestination(), r.getEstimatedCost(),
                            r.getEstimatedTransitHours(), r.getOperationalRiskScore(), r.getActive()
                    ));
                    if (i < routes.size() - 1) sb.append(",");
                }
                sb.append("]");
                out.write(sb.toString());
            } catch (Exception e) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.write(String.format("{\"status\":\"FORBIDDEN\",\"message\":\"%s\"}", e.getMessage()));
            }
            return;
        }

        if ("/route-optimizations".equals(pathInfo)) {
            if (!req.isUserInRole(SecurityRoles.ADMIN) && !req.isUserInRole(SecurityRoles.LOGISTICS_COORDINATOR)) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.write("{\"status\":\"FORBIDDEN\",\"message\":\"Access denied to route optimizations\"}");
                return;
            }
            try {
                List<com.jiat.globaltrade.entity.RouteOptimizationRecommendation> list = routeOptimizationService.findAllRecommendations();
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < list.size(); i++) {
                    com.jiat.globaltrade.entity.RouteOptimizationRecommendation r = list.get(i);
                    sb.append(String.format(
                            "{\"id\":%d,\"shipmentId\":%d,\"routeCode\":\"%s\",\"score\":%s,\"transitHours\":%d,\"cost\":%s,\"risk\":%s}",
                            r.getId(), r.getShipment().getId(), r.getSelectedRoute().getRouteCode(),
                            r.getOptimizationScore(), r.getTransitTimeHours(), r.getEstimatedCost(), r.getRiskScore()
                    ));
                    if (i < list.size() - 1) sb.append(",");
                }
                sb.append("]");
                out.write(sb.toString());
            } catch (Exception e) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.write(String.format("{\"status\":\"FORBIDDEN\",\"message\":\"%s\"}", e.getMessage()));
            }
            return;
        }

        if (pathInfo != null && pathInfo.startsWith("/route-optimizations/shipment/")) {
            if (!req.isUserInRole(SecurityRoles.ADMIN) && !req.isUserInRole(SecurityRoles.LOGISTICS_COORDINATOR)) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.write("{\"status\":\"FORBIDDEN\",\"message\":\"Access denied to route optimizations\"}");
                return;
            }
            String idStr = pathInfo.substring("/route-optimizations/shipment/".length());
            try {
                Long shipmentId = Long.parseLong(idStr);
                com.jiat.globaltrade.entity.RouteOptimizationRecommendation r = routeOptimizationService.findRecommendationByShipmentId(shipmentId);
                out.write(String.format(
                        "{\"id\":%d,\"shipmentId\":%d,\"routeCode\":\"%s\",\"score\":%s,\"transitHours\":%d,\"cost\":%s,\"risk\":%s}",
                        r.getId(), r.getShipment().getId(), r.getSelectedRoute().getRouteCode(),
                        r.getOptimizationScore(), r.getTransitTimeHours(), r.getEstimatedCost(), r.getRiskScore()
                ));
            } catch (com.jiat.globaltrade.exception.ResourceNotFoundException e) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.write(String.format("{\"status\":\"NOT_FOUND\",\"message\":\"%s\"}", e.getMessage()));
            } catch (Exception e) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.write(String.format("{\"status\":\"FORBIDDEN\",\"message\":\"%s\"}", e.getMessage()));
            }
            return;
        }

        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        out.write("{\"status\":\"NOT_FOUND\",\"message\":\"Unknown probe endpoint\"}");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null) {
            pathInfo = "/";
        }

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();

        if ("/route-options/seed-test-fixtures".equals(pathInfo)) {
            if (!req.isUserInRole(SecurityRoles.ADMIN)) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.write("{\"status\":\"FORBIDDEN\",\"message\":\"Caller is not in role ADMIN\"}");
                return;
            }
            boolean airDiscount = "true".equalsIgnoreCase(req.getParameter("airDiscount"));
            try {
                invoker.invokeRunnable(() -> {
                    List<com.jiat.globaltrade.entity.RouteOption> existing = routeOptimizationService.findRouteOptions("Tokyo, Japan", "Singapore", false);
                    com.jiat.globaltrade.entity.RouteOption std = null;
                    com.jiat.globaltrade.entity.RouteOption exp = null;
                    com.jiat.globaltrade.entity.RouteOption eco = null;
                    com.jiat.globaltrade.entity.RouteOption ina = null;
                    for (com.jiat.globaltrade.entity.RouteOption r : existing) {
                        if ("RT-TEST-TYO-SIN-STD".equals(r.getRouteCode())) std = r;
                        if ("RT-TEST-TYO-SIN-EXP".equals(r.getRouteCode())) exp = r;
                        if ("RT-TEST-TYO-SIN-ECO".equals(r.getRouteCode())) eco = r;
                        if ("RT-TEST-TYO-SIN-INA".equals(r.getRouteCode())) ina = r;
                    }

                    if (std == null) {
                        std = new com.jiat.globaltrade.entity.RouteOption("RT-TEST-TYO-SIN-STD", "Tokyo, Japan", "Singapore", "Pacific Maritime Express", "PME-204", "SEA", 72, new BigDecimal("900.00"), new BigDecimal("0.08"), true);
                        invoker.persist(std);
                    } else {
                        std.setEstimatedCost(new BigDecimal("900.00"));
                        std.setEstimatedTransitHours(72);
                        std.setOperationalRiskScore(new BigDecimal("0.08"));
                        invoker.merge(std);
                    }

                    if (eco == null) {
                        eco = new com.jiat.globaltrade.entity.RouteOption("RT-TEST-TYO-SIN-ECO", "Tokyo, Japan", "Singapore", "Ocean Alliance Line", "OAL-105", "SEA", 168, new BigDecimal("600.00"), new BigDecimal("0.20"), true);
                        invoker.persist(eco);
                    }

                    if (exp == null) {
                        exp = new com.jiat.globaltrade.entity.RouteOption("RT-TEST-TYO-SIN-EXP", "Tokyo, Japan", "Singapore", "Nippon Air Cargo", "NAC-801", "AIR", 18, airDiscount ? new BigDecimal("400.00") : new BigDecimal("3200.00"), airDiscount ? new BigDecimal("0.02") : new BigDecimal("0.05"), true);
                        invoker.persist(exp);
                    } else {
                        exp.setEstimatedCost(airDiscount ? new BigDecimal("400.00") : new BigDecimal("3200.00"));
                        exp.setOperationalRiskScore(airDiscount ? new BigDecimal("0.02") : new BigDecimal("0.05"));
                        invoker.merge(exp);
                    }

                    if (ina == null) {
                        ina = new com.jiat.globaltrade.entity.RouteOption("RT-TEST-TYO-SIN-INA", "Tokyo, Japan", "Singapore", "Phantom Fastline", "PFL-001", "AIR", 5, new BigDecimal("100.00"), new BigDecimal("0.01"), false);
                        invoker.persist(ina);
                    }
                });
                out.write("{\"status\":\"SUCCESS\",\"message\":\"Test fixtures seeded successfully.\"}");
            } catch (Exception e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.write(String.format("{\"status\":\"ERROR\",\"message\":\"%s\"}", e.getMessage()));
            }
            return;
        }

        if ("/route-optimizations/run".equals(pathInfo)) {
            if (!req.isUserInRole(SecurityRoles.ADMIN)) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.write("{\"status\":\"FORBIDDEN\",\"message\":\"Caller is not in role ADMIN\"}");
                return;
            }
            com.jiat.globaltrade.service.RouteOptimizationCoordinatorBean.RouteOptimizationBatchSummary summary =
                    routeCoordinator.optimizeAllActiveShipments("PROBE_TEST_BATCH", "ADMIN");
            out.write(String.format(
                    "{\"status\":\"%s\",\"total\":%d,\"success\":%d,\"failed\":%d,\"skipped\":%d}",
                    summary.getOverallStatus(), summary.getTotalShipmentsEvaluated(),
                    summary.getSuccessfulOptimizations(), summary.getFailedOptimizations(), summary.getSkippedShipments()
            ));
            return;
        }

        if (pathInfo != null && pathInfo.startsWith("/route-optimizations/evaluate/")) {
            if (!req.isUserInRole(SecurityRoles.ADMIN) && !req.isUserInRole(SecurityRoles.LOGISTICS_COORDINATOR)) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.write("{\"status\":\"FORBIDDEN\",\"message\":\"Access denied to evaluate route\"}");
                return;
            }
            String idStr = pathInfo.substring("/route-optimizations/evaluate/".length());
            try {
                Long shipmentId = Long.parseLong(idStr);
                String caller = req.getUserPrincipal() != null ? req.getUserPrincipal().getName() : "TEST_USER";
                com.jiat.globaltrade.entity.RouteOptimizationRecommendation r =
                        routeOptimizationService.optimizeShipmentRoute(shipmentId, "PROBE_TEST_EVALUATE", caller);
                if (r == null) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.write("{\"status\":\"SKIPPED\",\"message\":\"Shipment is not active\"}");
                } else {
                    out.write(String.format(
                            "{\"status\":\"SUCCESS\",\"id\":%d,\"shipmentId\":%d,\"routeCode\":\"%s\",\"score\":%s,\"transitHours\":%d,\"cost\":%s,\"risk\":%s}",
                            r.getId(), r.getShipment().getId(), r.getSelectedRoute().getRouteCode(),
                            r.getOptimizationScore(), r.getTransitTimeHours(), r.getEstimatedCost(), r.getRiskScore()
                    ));
                }
            } catch (com.jiat.globaltrade.exception.ResourceNotFoundException e) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.write(String.format("{\"status\":\"NOT_FOUND\",\"message\":\"%s\"}", e.getMessage()));
            } catch (Exception e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.write(String.format("{\"status\":\"ERROR\",\"message\":\"%s\"}", e.getMessage()));
            }
            return;
        }

        if ("/monitoring/run".equals(pathInfo)) {
            if (!req.isUserInRole(SecurityRoles.ADMIN)) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.write("{\"status\":\"FORBIDDEN\",\"message\":\"Caller is not in role ADMIN\"}");
                return;
            }
            com.jiat.globaltrade.service.SupplyChainMonitoringServiceBean.SupplyChainEvaluationResult result =
                    monitoringService.evaluateSupplyChain("PROBE_TEST_TRIGGER");
            out.write(String.format(
                    "{\"status\":\"%s\",\"successfulCategories\":%d,\"failedCategories\":%d,\"activeAlerts\":%d,\"resolvedAlerts\":%d}",
                    result.getOverallStatus(), result.getSuccessfulCategories(), result.getFailedCategories(),
                    result.getTotalActiveAlertsDetected(), result.getTotalAlertsResolved()
            ));
            return;
        }

        if (pathInfo != null && pathInfo.startsWith("/alerts/acknowledge/")) {
            String idStr = pathInfo.substring("/alerts/acknowledge/".length());
            try {
                Long alertId = Long.parseLong(idStr);
                com.jiat.globaltrade.entity.SupplyChainAlert a = alertService.acknowledgeAlert(alertId);
                out.write(String.format(
                        "{\"status\":\"SUCCESS\",\"id\":%d,\"alertKey\":\"%s\",\"alertStatus\":\"%s\",\"acknowledgedBy\":\"%s\"}",
                        a.getId(), a.getAlertKey(), a.getAlertStatus(), a.getAcknowledgedBy()
                ));
            } catch (Exception e) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.write(String.format("{\"status\":\"FORBIDDEN\",\"message\":\"%s\"}", e.getMessage()));
            }
            return;
        }

        if ("/ui-login".equals(pathInfo)) {
            StringBuilder sb = new StringBuilder();
            try (java.io.BufferedReader reader = req.getReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            String body = sb.toString();
            String username = extractJsonString(body, "username");
            String password = extractJsonString(body, "password");

            if (username == null || password == null) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.write("{\"status\":\"UNAUTHORIZED\",\"message\":\"Username and password are required.\"}");
                return;
            }

            try {
                try {
                    if (req.getUserPrincipal() != null) {
                        req.logout();
                    }
                } catch (Exception ignored) {
                }

                req.login(username, password);

                Principal p = req.getUserPrincipal();
                String principalName = p != null ? p.getName() : username;
                boolean isAdmin = req.isUserInRole(SecurityRoles.ADMIN);
                boolean isCustomer = req.isUserInRole(SecurityRoles.CUSTOMER);

                out.write(String.format(
                        "{\"status\":\"SUCCESS\",\"authenticated\":true,\"principal\":\"%s\"," +
                        "\"roles\":{\"ADMIN\":%b,\"CUSTOMER\":%b}}",
                        principalName, isAdmin, isCustomer
                ));
            } catch (jakarta.servlet.ServletException e) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.write("{\"status\":\"UNAUTHORIZED\",\"message\":\"Invalid username or password.\"}");
            }
            return;
        }

        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        out.write("{\"status\":\"NOT_FOUND\",\"message\":\"Unknown probe endpoint\"}");
    }

    private String extractJsonString(String json, String key) {
        if (json == null) return null;
        String pattern = "\"" + key + "\":\"";
        int start = json.indexOf(pattern);
        if (start == -1) return null;
        start += pattern.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return null;
        return json.substring(start, end);
    }
}

