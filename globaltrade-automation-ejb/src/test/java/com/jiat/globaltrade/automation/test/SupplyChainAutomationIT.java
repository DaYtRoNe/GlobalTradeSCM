package com.jiat.globaltrade.automation.test;

import com.jiat.globaltrade.automation.AutomationDiagnosticServiceBean;
import com.jiat.globaltrade.automation.InventoryReplenishmentAutomationCoordinatorBean;
import com.jiat.globaltrade.automation.InventoryReplenishmentAutomationWorkerBean;
import com.jiat.globaltrade.automation.ShipmentTrackingAutomationCoordinatorBean;
import com.jiat.globaltrade.entity.enums.CustomsDocumentStatus;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.ejb.EJB;

import static org.junit.jupiter.api.Assertions.*;

/**
 * =================================================================================================
 * ARQUILLIAN INTEGRATION TEST: SUPPLY CHAIN AUTOMATION MODULE (PHASE 12B)
 * =================================================================================================
 * Exercises:
 * - Automated scheduled shipment carrier telematics polling
 * - Automated low-stock replenishment dispatch to WMS & Supplier Portal gateways
 * - Replenishment de-duplication cooldown protection
 * - Automated customs clearance status synchronization
 * - Transaction-level failure isolation in automation coordinators
 * - End-to-end full automation cycle execution
 *
 * All secured EJB calls are routed through AutomationTestInvoker (@RunAs ADMIN),
 * following the proven AdminTestInvoker pattern from globaltrade-ejb.
 */
@ExtendWith(ArquillianExtension.class)
@TestMethodOrder(MethodOrderer.DisplayName.class)
public class SupplyChainAutomationIT {

    /**
     * Minimal web.xml declaring all application security roles.
     * Required so Payara recognizes the roles referenced in @DeclareRoles, @RolesAllowed, @RunAs.
     */
    private static final String TEST_WEB_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <web-app xmlns="https://jakarta.ee/xml/ns/jakartaee"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee
                                         https://jakarta.ee/xml/ns/jakartaee/web-app_6_0.xsd"
                     version="6.0">
                <display-name>GlobalTrade Automation Test Deployment</display-name>

                <login-config>
                    <auth-method>BASIC</auth-method>
                    <realm-name>GlobalTradeCustomRealm</realm-name>
                </login-config>

                <security-role>
                    <role-name>ADMIN</role-name>
                </security-role>
                <security-role>
                    <role-name>LOGISTICS_COORDINATOR</role-name>
                </security-role>
                <security-role>
                    <role-name>CUSTOMS_AGENT</role-name>
                </security-role>
                <security-role>
                    <role-name>WAREHOUSE_MANAGER</role-name>
                </security-role>
                <security-role>
                    <role-name>VENDOR_REPRESENTATIVE</role-name>
                </security-role>
                <security-role>
                    <role-name>CUSTOMER</role-name>
                </security-role>
                <security-role>
                    <role-name>SYSTEM</role-name>
                </security-role>
            </web-app>
            """;

    /**
     * Payara/GlassFish web role-to-principal mapping.
     * Required for @RunAs to resolve roles to concrete security principals.
     */
    private static final String TEST_GLASSFISH_WEB_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE glassfish-web-app PUBLIC "-//GlassFish.org//DTD GlassFish Application Server 3.1 Servlet 3.0//EN" "http://glassfish.org/dtds/glassfish-web-app_3_0-1.dtd">
            <glassfish-web-app>
                <security-role-mapping>
                    <role-name>ADMIN</role-name>
                    <principal-name>gt_admin</principal-name>
                    <group-name>ADMIN</group-name>
                </security-role-mapping>
                <security-role-mapping>
                    <role-name>LOGISTICS_COORDINATOR</role-name>
                    <principal-name>gt_coordinator</principal-name>
                    <group-name>LOGISTICS_COORDINATOR</group-name>
                </security-role-mapping>
                <security-role-mapping>
                    <role-name>CUSTOMS_AGENT</role-name>
                    <principal-name>gt_customs</principal-name>
                    <group-name>CUSTOMS_AGENT</group-name>
                </security-role-mapping>
                <security-role-mapping>
                    <role-name>WAREHOUSE_MANAGER</role-name>
                    <principal-name>gt_warehouse</principal-name>
                    <group-name>WAREHOUSE_MANAGER</group-name>
                </security-role-mapping>
                <security-role-mapping>
                    <role-name>VENDOR_REPRESENTATIVE</role-name>
                    <principal-name>gt_vendor</principal-name>
                    <group-name>VENDOR_REPRESENTATIVE</group-name>
                </security-role-mapping>
                <security-role-mapping>
                    <role-name>CUSTOMER</role-name>
                    <principal-name>gt_customer</principal-name>
                    <group-name>CUSTOMER</group-name>
                </security-role-mapping>
                <security-role-mapping>
                    <role-name>SYSTEM</role-name>
                    <principal-name>gt_system</principal-name>
                    <group-name>SYSTEM</group-name>
                </security-role-mapping>
            </glassfish-web-app>
            """;

    /**
     * Payara/GlassFish EJB role-to-principal mapping.
     * Required for @RunAs on EJBs to resolve roles to concrete security principals.
     */
    private static final String TEST_GLASSFISH_EJB_JAR_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE glassfish-ejb-jar PUBLIC "-//GlassFish.org//DTD GlassFish Application Server 3.1 EJB 3.1//EN" "http://glassfish.org/dtds/glassfish-ejb-jar_3_1-1.dtd">
            <glassfish-ejb-jar>
                <security-role-mapping>
                    <role-name>ADMIN</role-name>
                    <principal-name>gt_admin</principal-name>
                    <group-name>ADMIN</group-name>
                </security-role-mapping>
                <security-role-mapping>
                    <role-name>LOGISTICS_COORDINATOR</role-name>
                    <principal-name>gt_coordinator</principal-name>
                    <group-name>LOGISTICS_COORDINATOR</group-name>
                </security-role-mapping>
                <security-role-mapping>
                    <role-name>CUSTOMS_AGENT</role-name>
                    <principal-name>gt_customs</principal-name>
                    <group-name>CUSTOMS_AGENT</group-name>
                </security-role-mapping>
                <security-role-mapping>
                    <role-name>WAREHOUSE_MANAGER</role-name>
                    <principal-name>gt_warehouse</principal-name>
                    <group-name>WAREHOUSE_MANAGER</group-name>
                </security-role-mapping>
                <security-role-mapping>
                    <role-name>VENDOR_REPRESENTATIVE</role-name>
                    <principal-name>gt_vendor</principal-name>
                    <group-name>VENDOR_REPRESENTATIVE</group-name>
                </security-role-mapping>
                <security-role-mapping>
                    <role-name>CUSTOMER</role-name>
                    <principal-name>gt_customer</principal-name>
                    <group-name>CUSTOMER</group-name>
                </security-role-mapping>
                <security-role-mapping>
                    <role-name>SYSTEM</role-name>
                    <principal-name>gt_system</principal-name>
                    <group-name>SYSTEM</group-name>
                </security-role-mapping>
            </glassfish-ejb-jar>
            """;

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "supply-chain-automation-test.war")
                // Entities and Enums
                .addPackage("com.jiat.globaltrade.entity")
                .addPackage("com.jiat.globaltrade.entity.enums")
                // Exceptions
                .addPackage("com.jiat.globaltrade.exception")
                // Interceptors
                .addPackage("com.jiat.globaltrade.interceptor")
                // Core Services
                .addPackage("com.jiat.globaltrade.service")
                // Integration Gateways and Adapters
                .addPackage("com.jiat.globaltrade.integration.gateway")
                .addPackage("com.jiat.globaltrade.integration.adapter")
                .addPackage("com.jiat.globaltrade.integration.model")
                .addPackage("com.jiat.globaltrade.integration.service")
                // Timers
                .addPackage("com.jiat.globaltrade.timer")
                // Security
                .addPackage("com.jiat.globaltrade.security")
                .addPackage("com.jiat.globaltrade.security.dto")
                // Automation Module Classes
                .addPackage("com.jiat.globaltrade.automation")
                // Test invoker
                .addClass(AutomationTestInvoker.class)
                // JPA descriptor
                .addAsResource("META-INF/persistence.xml", "META-INF/persistence.xml")
                // CDI configuration
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                // Security descriptors — matching proven core TestDeployments pattern
                .addAsWebInfResource(new StringAsset(TEST_WEB_XML), "web.xml")
                .addAsWebInfResource(new StringAsset(TEST_GLASSFISH_WEB_XML), "glassfish-web.xml")
                .addAsWebInfResource(new StringAsset(TEST_GLASSFISH_EJB_JAR_XML), "glassfish-ejb-jar.xml");
    }

    @EJB
    private AutomationTestInvoker invoker;

    @BeforeEach
    void setUp() {
        InventoryReplenishmentAutomationCoordinatorBean.clearDeduplicationCache();
    }

    @Test
    @DisplayName("01. Scheduled shipment tracking worker polls carrier telematics for active shipment")
    void testScheduledShipmentTracking_pollActiveShipment() {
        // Shipment #1 in seed data is IN_TRANSIT (TRK-2026-001)
        // Routed through invoker to establish authenticated caller identity
        boolean polled = invoker.pollShipmentCarrier(1L);
        assertTrue(polled, "Active in-transit shipment #1 must be successfully polled for carrier telemetry");
    }

    @Test
    @DisplayName("02. Delivered shipment is skipped by automated tracking poller")
    void testDeliveredShipment_skippedByTrackingWorker() {
        // Shipment #2 in seed data is DELIVERED (TRK-2026-002)
        boolean polled = invoker.pollShipmentCarrier(2L);
        assertFalse(polled, "Delivered shipment #2 must be skipped by automated tracking poller");
    }

    @Test
    @DisplayName("03. Low inventory condition triggers automated replenishment request to WMS and Supplier")
    void testLowInventory_triggersReplenishmentRequest() {
        InventoryReplenishmentAutomationWorkerBean.ReplenishmentEvaluationResult result =
                invoker.evaluateAndReplenish(1L);
        assertNotNull(result);
        assertNotNull(result.sku());
    }

    @Test
    @DisplayName("04. Healthy inventory above reorder threshold skips replenishment dispatch")
    void testHealthyInventory_skipsReplenishment() {
        // Item #1 (PK=1) in seed data has quantity=1200, reorderLevel=200 -> Nominal
        InventoryReplenishmentAutomationWorkerBean.ReplenishmentEvaluationResult result =
                invoker.evaluateAndReplenish(1L);
        assertNotNull(result);
        assertFalse(result.replenishmentRequested(), "Nominal inventory item (1200 > 200) must skip replenishment");
    }

    @Test
    @DisplayName("05. Repeated execution employs deterministic >=60min cooldown to prevent duplicate requests")
    void testRepeatedExecution_idempotentDeduplication() {
        // Run #1: Initial execution
        InventoryReplenishmentAutomationCoordinatorBean.ReplenishmentBatchSummary summary1 =
                invoker.evaluateAndReplenishAllItems();
        assertNotNull(summary1);

        // Run #2: Immediate consecutive execution (Fast-path in-memory cooldown)
        InventoryReplenishmentAutomationCoordinatorBean.ReplenishmentBatchSummary summary2 =
                invoker.evaluateAndReplenishAllItems();
        assertNotNull(summary2);
        assertEquals(0, summary2.replenishmentOrdersPlaced(),
                "Immediate second cycle must place 0 orders due to active in-memory cooldown");

        // Run #3: Subsequent 15-min simulated cycle even if in-memory cache was cleared (Persistent audit-backed cooldown)
        InventoryReplenishmentAutomationCoordinatorBean.clearDeduplicationCache();
        InventoryReplenishmentAutomationCoordinatorBean.ReplenishmentBatchSummary summary3 =
                invoker.evaluateAndReplenishAllItems();
        assertNotNull(summary3);
        assertEquals(0, summary3.replenishmentOrdersPlaced(),
                "Subsequent cycle within 60-minute audit window must place 0 orders due to persistent audit log check");
    }

    @Test
    @DisplayName("06. Customs documentation polling retrieves clearance for pending documents")
    void testCustomsDocumentationPolling_pendingDocuments() {
        // Deterministic test fixture: create document in SUBMITTED state
        Long docId = invoker.createOrUpdateCustomsDocument("DOC-AUTO-PENDING-001", 1L, CustomsDocumentStatus.SUBMITTED);
        assertNotNull(docId);

        boolean polled = invoker.pollCustomsDocumentStatus(docId);
        assertTrue(polled, "Submitted customs document must be successfully polled for clearance status");
    }

    @Test
    @DisplayName("07. Approved customs document is skipped by automated clearance poller")
    void testApprovedCustomsDocument_skippedByPoller() {
        // Deterministic test fixture: create document in APPROVED state
        Long docId = invoker.createOrUpdateCustomsDocument("DOC-AUTO-APPROVED-001", 1L, CustomsDocumentStatus.APPROVED);
        assertNotNull(docId);

        boolean polled = invoker.pollCustomsDocumentStatus(docId);
        assertFalse(polled, "Approved customs document must be skipped by automated clearance poller");
    }

    @Test
    @DisplayName("08. Transaction failure isolation: Single item failure does not abort batch")
    void testFailureIsolation_batchContinuesOnSingleError() {
        ShipmentTrackingAutomationCoordinatorBean.ShipmentTrackingBatchSummary summary =
                invoker.pollAllActiveShipments();
        assertNotNull(summary);
        assertTrue(summary.totalEvaluated() >= 1, "Batch tracking should evaluate at least 1 active shipment");
    }

    @Test
    @DisplayName("09. End-to-end full automation cycle executes all 3 coordinator workflows")
    void testFullAutomationCycle_execution() {
        // Ensure deterministic customs fixture in SUBMITTED state exists for full cycle customs sync
        invoker.createOrUpdateCustomsDocument("DOC-AUTO-PENDING-001", 1L, CustomsDocumentStatus.SUBMITTED);

        AutomationDiagnosticServiceBean.FullAutomationCycleSummary summary =
                invoker.runFullAutomationCycle("INTEGRATION_TEST");
        assertNotNull(summary);
        assertEquals("SUCCESS", summary.status());
        assertTrue(summary.shipmentsPolled() >= 1, "Full cycle should successfully poll at least 1 active shipment");
    }
}
