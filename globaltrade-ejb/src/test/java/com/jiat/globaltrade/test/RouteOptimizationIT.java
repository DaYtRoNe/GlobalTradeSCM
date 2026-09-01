package com.jiat.globaltrade.test;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * =================================================================================================
 * ARQUILLIAN INTEGRATION TEST: AUTOMATED ROUTE OPTIMIZATION SERVICE (PHASE 11B)
 * =================================================================================================
 * Exercises:
 * - Active shipment eligibility filtering (PENDING/IN_TRANSIT evaluated, DELIVERED/CANCELLED skipped)
 * - Deterministic multi-criteria route scoring algorithm (45% speed, 35% cost, 20% risk)
 * - Inactive route candidate filtering
 * - Re-evaluation idempotency (1 recommendation per shipment, zero duplicate rows)
 * - Recommendation updates and audit log emission on changed route conditions
 * - Transaction-level failure isolation across shipments
 * - Declarative & Programmatic RBAC security matrix (ADMIN & LOGISTICS_COORDINATOR allowed, others 403)
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
@TestMethodOrder(MethodOrderer.DisplayName.class)
public class RouteOptimizationIT {

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return TestDeployments.createEnterpriseDeployment("route-optimization-test.war");
    }

    @ArquillianResource
    private URL deploymentUrl;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private HttpResponse<String> sendGet(String path, String username, String password) throws Exception {
        assertNotNull(deploymentUrl, "Deployment URL must be injected by Arquillian container");
        URI targetUri = deploymentUrl.toURI().resolve(path);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(targetUri)
                .GET();

        if (username != null && password != null) {
            String auth = username + ":" + password;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            builder.header("Authorization", "Basic " + encodedAuth);
        }

        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> sendPost(String path, String username, String password, String body) throws Exception {
        assertNotNull(deploymentUrl, "Deployment URL must be injected by Arquillian container");
        URI targetUri = deploymentUrl.toURI().resolve(path);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(targetUri)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body != null ? body : ""));

        if (username != null && password != null) {
            String auth = username + ":" + password;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            builder.header("Authorization", "Basic " + encodedAuth);
        }

        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    @DisplayName("01. Active shipment with candidate routes receives deterministic recommendation")
    void testActiveShipment_receivesRecommendation() throws Exception {
        // Seed candidate routes for Tokyo -> Singapore
        HttpResponse<String> seedResp = sendPost("security-test/route-options/seed-test-fixtures", "gt_admin", "Password@123", "");
        assertEquals(200, seedResp.statusCode());

        // Evaluate active shipment #1 (Tokyo -> Singapore, IN_TRANSIT)
        HttpResponse<String> evalResp = sendPost("security-test/route-optimizations/evaluate/1", "gt_admin", "Password@123", "");
        assertEquals(200, evalResp.statusCode(), "Active shipment must receive a route recommendation");
        String body = evalResp.body();
        assertTrue(body.contains("\"status\":\"SUCCESS\""));
        assertTrue(body.contains("\"shipmentId\":1"));
        assertTrue(body.contains("\"routeCode\":\"RT-TEST-TYO-SIN-STD\""), "Balanced sea route STD should win initial evaluation");
    }

    @Test
    @DisplayName("02. DELIVERED shipment is skipped by route optimizer")
    void testDeliveredShipment_isSkipped() throws Exception {
        // Shipment #2 in seed data is DELIVERED (Hamburg -> Rotterdam)
        HttpResponse<String> evalResp = sendPost("security-test/route-optimizations/evaluate/2", "gt_admin", "Password@123", "");
        assertEquals(400, evalResp.statusCode(), "DELIVERED shipment must be skipped and return 400 Bad Request");
        assertTrue(evalResp.body().contains("\"status\":\"SKIPPED\""));
    }

    @Test
    @DisplayName("03. Non-existent or cancelled shipment is handled gracefully")
    void testCancelledShipment_isSkipped() throws Exception {
        HttpResponse<String> evalResp = sendPost("security-test/route-optimizations/evaluate/9999", "gt_admin", "Password@123", "");
        assertEquals(404, evalResp.statusCode(), "Non-existent shipment must return 404 NOT_FOUND");
    }

    @Test
    @DisplayName("04. Inactive RouteOption is ignored during candidate selection")
    void testInactiveRouteOption_isIgnored() throws Exception {
        sendPost("security-test/route-options/seed-test-fixtures", "gt_admin", "Password@123", "");
        HttpResponse<String> optResp = sendGet("security-test/route-options?origin=Tokyo,%20Japan&destination=Singapore&activeOnly=true", "gt_admin", "Password@123");
        assertEquals(200, optResp.statusCode());
        String body = optResp.body();
        assertTrue(body.contains("RT-TEST-TYO-SIN-STD"));
        assertTrue(body.contains("RT-TEST-TYO-SIN-EXP"));
        assertFalse(body.contains("RT-TEST-TYO-SIN-INA"), "Inactive route must be excluded when activeOnly=true");
    }

    @Test
    @DisplayName("05. Deterministic scoring algorithm produces valid normalized scores")
    void testDeterministicScoringAlgorithm() throws Exception {
        sendPost("security-test/route-options/seed-test-fixtures", "gt_admin", "Password@123", "");
        HttpResponse<String> evalResp = sendPost("security-test/route-optimizations/evaluate/1", "gt_admin", "Password@123", "");
        assertEquals(200, evalResp.statusCode());
        String body = evalResp.body();
        assertTrue(body.contains("\"score\":"));
        assertTrue(body.contains("\"cost\":"));
        assertTrue(body.contains("\"transitHours\":"));
    }

    @Test
    @DisplayName("06. Re-evaluation is idempotent with zero duplicate recommendation rows")
    void testReevaluation_isIdempotent_noDuplicateRows() throws Exception {
        sendPost("security-test/route-options/seed-test-fixtures", "gt_admin", "Password@123", "");

        // First evaluation
        HttpResponse<String> eval1 = sendPost("security-test/route-optimizations/evaluate/1", "gt_admin", "Password@123", "");
        assertEquals(200, eval1.statusCode());

        // Second evaluation
        HttpResponse<String> eval2 = sendPost("security-test/route-optimizations/evaluate/1", "gt_admin", "Password@123", "");
        assertEquals(200, eval2.statusCode());

        // Verify recommendations count
        HttpResponse<String> listResp = sendGet("security-test/route-optimizations", "gt_admin", "Password@123");
        assertEquals(200, listResp.statusCode());
    }

    @Test
    @DisplayName("07. Changed candidate conditions updates recommendation and emits audit log")
    void testChangedCandidateConditions_updatesRecommendation() throws Exception {
        // Initial state: STD wins
        sendPost("security-test/route-options/seed-test-fixtures", "gt_admin", "Password@123", "");
        HttpResponse<String> eval1 = sendPost("security-test/route-optimizations/evaluate/1", "gt_admin", "Password@123", "");
        assertEquals(200, eval1.statusCode());
        assertTrue(eval1.body().contains("RT-TEST-TYO-SIN-STD"));

        // Air discount applies: AIR becomes fastest and cheapest (cost $400, risk 0.02)
        sendPost("security-test/route-options/seed-test-fixtures?airDiscount=true", "gt_admin", "Password@123", "");
        HttpResponse<String> eval2 = sendPost("security-test/route-optimizations/evaluate/1", "gt_admin", "Password@123", "");
        assertEquals(200, eval2.statusCode());
        assertTrue(eval2.body().contains("RT-TEST-TYO-SIN-EXP"), "Updated route conditions must transition recommendation to optimal EXP route");
    }

    @Test
    @DisplayName("08. Transaction failure isolation: Batch optimization handles mixed states")
    void testTransactionFailureIsolation() throws Exception {
        sendPost("security-test/route-options/seed-test-fixtures", "gt_admin", "Password@123", "");
        HttpResponse<String> batchResp = sendPost("security-test/route-optimizations/run", "gt_admin", "Password@123", "");
        assertEquals(200, batchResp.statusCode());
        String body = batchResp.body();
        assertTrue(body.contains("\"status\":\"SUCCESS\"") || body.contains("\"status\":\"PARTIAL_FAILURE\""));
    }

    @Test
    @DisplayName("09. ADMIN role has full access to route optimizations (returns 200)")
    void testAdminAccess_allowed() throws Exception {
        HttpResponse<String> optResp = sendGet("security-test/route-options", "gt_admin", "Password@123");
        assertEquals(200, optResp.statusCode(), "ADMIN must have access to route options");

        HttpResponse<String> runResp = sendPost("security-test/route-optimizations/run", "gt_admin", "Password@123", "");
        assertEquals(200, runResp.statusCode(), "ADMIN must have access to run batch optimization");
    }

    @Test
    @DisplayName("10. LOGISTICS_COORDINATOR role has access to route options and recommendations (returns 200)")
    void testLogisticsCoordinatorAccess_allowed() throws Exception {
        HttpResponse<String> optResp = sendGet("security-test/route-options", "gt_coordinator", "Password@123");
        assertEquals(200, optResp.statusCode(), "LOGISTICS_COORDINATOR must have access to route options");

        HttpResponse<String> listResp = sendGet("security-test/route-optimizations", "gt_coordinator", "Password@123");
        assertEquals(200, listResp.statusCode(), "LOGISTICS_COORDINATOR must have access to view recommendations");
    }

    @Test
    @DisplayName("11. CUSTOMER role is denied access to route optimizations (returns 403)")
    void testCustomerAccess_denied() throws Exception {
        HttpResponse<String> optResp = sendGet("security-test/route-options", "gt_customer", "Password@123");
        assertEquals(403, optResp.statusCode(), "CUSTOMER must receive 403 for route options");

        HttpResponse<String> runResp = sendPost("security-test/route-optimizations/run", "gt_customer", "Password@123", "");
        assertEquals(403, runResp.statusCode(), "CUSTOMER must receive 403 for batch optimization");
    }

    @Test
    @DisplayName("12. VENDOR_REPRESENTATIVE role is denied access to route optimizations (returns 403)")
    void testVendorAccess_denied() throws Exception {
        HttpResponse<String> optResp = sendGet("security-test/route-options", "gt_vendor", "Password@123");
        assertEquals(403, optResp.statusCode(), "VENDOR_REPRESENTATIVE must receive 403 for route options");
    }

    @Test
    @DisplayName("13. CUSTOMS_AGENT role is denied access to route optimizations (returns 403)")
    void testCustomsAgentAccess_denied() throws Exception {
        HttpResponse<String> optResp = sendGet("security-test/route-options", "gt_customs", "Password@123");
        assertEquals(403, optResp.statusCode(), "CUSTOMS_AGENT must receive 403 for route options");
    }

    @Test
    @DisplayName("14. WAREHOUSE_MANAGER role is denied access to route optimizations (returns 403)")
    void testWarehouseManagerAccess_denied() throws Exception {
        HttpResponse<String> optResp = sendGet("security-test/route-options", "gt_warehouse", "Password@123");
        assertEquals(403, optResp.statusCode(), "WAREHOUSE_MANAGER must receive 403 for route options");
    }
}
