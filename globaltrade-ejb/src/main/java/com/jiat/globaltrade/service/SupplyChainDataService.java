package com.jiat.globaltrade.service;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class SupplyChainDataService {

    private static final Logger LOGGER = Logger.getLogger(SupplyChainDataService.class.getName());

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    public boolean isDatabaseConnected() {
        if (em == null) {
            LOGGER.log(Level.SEVERE, "[SupplyChainDataService] EntityManager is null. PersistenceContext injection failed for unitName 'GlobalTradePU'.");
            return false;
        }
        try {
            LOGGER.log(Level.INFO, "[SupplyChainDataService] Verifying database connectivity via EntityManager (GlobalTradePU)...");
            Object result = em.createNativeQuery("SELECT 1").getSingleResult();
            LOGGER.log(Level.INFO, "[SupplyChainDataService] Database connectivity verified successfully. Result: {0}", result);
            return result != null;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[SupplyChainDataService] Database connectivity verification failed: " + e.getMessage(), e);
            return false;
        }
    }

    public long getVendorCount() {
        if (em == null) {
            LOGGER.log(Level.SEVERE, "[SupplyChainDataService] EntityManager is null when attempting to query vendor count.");
            return -1L;
        }
        try {
            Long count = em.createQuery("SELECT COUNT(v) FROM Vendor v", Long.class).getSingleResult();
            LOGGER.log(Level.INFO, "[SupplyChainDataService] Vendor count query executed successfully. Count: {0}", count);
            return count != null ? count : 0L;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[SupplyChainDataService] Failed to execute vendor count query: " + e.getMessage(), e);
            return -1L;
        }
    }

    public String getPersistenceStatus() {
        if (em == null) {
            LOGGER.log(Level.WARNING, "[SupplyChainDataService] EntityManager is null.");
            return "EntityManager is not injected.";
        }
        return "EntityManager (GlobalTradePU) is active and connected.";
    }
}

