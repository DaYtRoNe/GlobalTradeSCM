package com.jiat.globaltrade.test;

import com.jiat.globaltrade.service.SupplyChainDataService;
import jakarta.ejb.EJB;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 7B-1 Integration Test: Verifies JPA / EclipseLink persistence connectivity,
 * EntityManager injection, and database query execution inside Payara Server.
 */
@ExtendWith(ArquillianExtension.class)
public class PersistenceIntegrationIT {

    @Deployment
    public static WebArchive createDeployment() {
        return TestDeployments.createEnterpriseDeployment("persistence-test.war");
    }

    @EJB
    private SupplyChainDataService supplyChainDataService;

    @Test
    @DisplayName("Should successfully verify live database connectivity through EntityManager")
    void shouldVerifyDatabaseConnectivity() {
        assertNotNull(supplyChainDataService, "SupplyChainDataService must be injected by container");
        boolean isConnected = supplyChainDataService.isDatabaseConnected();
        assertTrue(isConnected, "Database connection verification query (SELECT 1) should succeed");
    }

    @Test
    @DisplayName("Should query vendor entity count from database successfully")
    void shouldQueryVendorCountSuccessfully() {
        assertNotNull(supplyChainDataService, "SupplyChainDataService must be injected by container");
        long vendorCount = supplyChainDataService.getVendorCount();
        assertTrue(vendorCount >= 1, "Vendor count in database should be non-zero (found: " + vendorCount + ")");
    }

    @Test
    @DisplayName("Should confirm active persistence unit GlobalTradePU status")
    void shouldConfirmPersistenceUnitStatus() {
        assertNotNull(supplyChainDataService, "SupplyChainDataService must be injected by container");
        String status = supplyChainDataService.getPersistenceStatus();
        assertNotNull(status, "Persistence status string must not be null");
        assertTrue(status.contains("GlobalTradePU"), "Status should reference GlobalTradePU persistence unit");
    }
}
