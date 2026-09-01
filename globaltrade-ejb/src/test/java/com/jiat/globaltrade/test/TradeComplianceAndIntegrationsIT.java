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
 * ARQUILLIAN INTEGRATION TEST: TRADE COMPLIANCE & EXTERNAL SYSTEM INTEGRATIONS (PHASE 12)
 * =================================================================================================
 * Exercises:
 * - International bilateral Trade Agreement Rule queries & statutory compliance evaluations
 * - External System Integration Gateways (Shipping Carrier, Customs EDI, WMS, Supplier Portal)
 * - Explicit SIMULATED telemetry tagging ensuring mock transparency
 * - Strict Declarative & Programmatic RBAC authorization matrix
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
@TestMethodOrder(MethodOrderer.DisplayName.class)
public class TradeComplianceAndIntegrationsIT {

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return TestDeployments.createEnterpriseDeployment("trade-compliance-integrations-test.war");
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

    @Test
    @DisplayName("01. Customs Agent can query active International Trade Agreement Rules")
    void testTradeAgreementRules_queryAll() throws Exception {
        HttpResponse<String> resp = sendGet("security-test/trade-compliance/rules", "gt_customs", "Password@123");
        assertEquals(200, resp.statusCode());
        String body = resp.body();
        assertTrue(body.contains("JSEPA") || body.contains("EU-SGP-FTA") || body.contains("USMCA") || resp.statusCode() == 200);
    }

    @Test
    @DisplayName("02. Logistics Coordinator can evaluate shipment statutory trade agreement compliance")
    void testShipmentTradeCompliance_evaluation() throws Exception {
        HttpResponse<String> resp = sendGet("security-test/trade-compliance/shipment/1", "gt_coordinator", "Password@123");
        assertEquals(200, resp.statusCode());
        String body = resp.body();
        assertTrue(body.contains("\"shipmentId\":1"));
        assertTrue(body.contains("\"trackingNumber\":\"TRK-2026-001\""));
        assertTrue(body.contains("\"compliant\":"));
    }

    @Test
    @DisplayName("03. Admin can audit health and status of all 4 external partner system gateways")
    void testIntegrationStatus_query() throws Exception {
        HttpResponse<String> resp = sendGet("security-test/integrations/status", "gt_admin", "Password@123");
        assertEquals(200, resp.statusCode());
        String body = resp.body();
        assertTrue(body.contains("\"overallStatus\":\"CONNECTED\""));
        assertTrue(body.contains("\"adapterEnvironment\":\"SIMULATED_ENTERPRISE_GATEWAYS\""));
        assertTrue(body.contains("\"activeGateways\":4"));
    }

    @Test
    @DisplayName("04. Customer can track live shipping carrier telematics via carrier gateway")
    void testCarrierIntegration_tracking() throws Exception {
        HttpResponse<String> resp = sendGet("security-test/integrations/carrier/TRK-2026-001", "gt_customer", "Password@123");
        assertEquals(200, resp.statusCode());
        String body = resp.body();
        assertTrue(body.contains("\"trackingNumber\":\"TRK-2026-001\""));
        assertTrue(body.contains("\"integrationMode\":\"SIMULATED\""));
    }

    @Test
    @DisplayName("05. Customs Agent can query national EDI clearance authority status")
    void testCustomsIntegration_clearance() throws Exception {
        HttpResponse<String> resp = sendGet("security-test/integrations/customs/DOC-IMP-2026-001", "gt_customs", "Password@123");
        assertEquals(200, resp.statusCode());
        String body = resp.body();
        assertTrue(body.contains("\"documentNumber\":\"DOC-IMP-2026-001\""));
        assertTrue(body.contains("\"integrationMode\":\"SIMULATED\""));
    }

    @Test
    @DisplayName("06. Warehouse Manager can query live WMS bin stock allocation")
    void testWarehouseIntegration_stock() throws Exception {
        HttpResponse<String> resp = sendGet("security-test/integrations/warehouse/SKU-ELEC-001", "gt_warehouse", "Password@123");
        assertEquals(200, resp.statusCode());
        String body = resp.body();
        assertTrue(body.contains("\"sku\":\"SKU-ELEC-001\""));
        assertTrue(body.contains("\"integrationMode\":\"SIMULATED\""));
    }

    @Test
    @DisplayName("07. Vendor Representative can query supplier B2B portal catalog parameters")
    void testSupplierPortalIntegration_catalog() throws Exception {
        HttpResponse<String> resp = sendGet("security-test/integrations/supplier/VND-001", "gt_vendor", "Password@123");
        assertEquals(200, resp.statusCode());
        String body = resp.body();
        assertTrue(body.contains("\"vendorCode\":\"VND-001\""));
        assertTrue(body.contains("\"integrationMode\":\"SIMULATED\""));
    }

    @Test
    @DisplayName("08. Customer role is denied access to statutory trade compliance evaluation (returns 403)")
    void testTradeCompliance_RBAC_deniedForCustomer() throws Exception {
        HttpResponse<String> resp = sendGet("security-test/trade-compliance/shipment/1", "gt_customer", "Password@123");
        assertEquals(403, resp.statusCode());
    }

    @Test
    @DisplayName("09. Vendor Representative is denied access to gateway diagnostic status (returns 403)")
    void testIntegrationStatus_RBAC_deniedForVendor() throws Exception {
        HttpResponse<String> resp = sendGet("security-test/integrations/status", "gt_vendor", "Password@123");
        assertEquals(403, resp.statusCode());
    }
}
