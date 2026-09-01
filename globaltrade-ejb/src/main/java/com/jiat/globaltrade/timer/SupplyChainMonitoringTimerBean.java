package com.jiat.globaltrade.timer;

import com.jiat.globaltrade.service.SupplyChainMonitoringServiceBean;
import com.jiat.globaltrade.service.SupplyChainMonitoringServiceBean.SupplyChainEvaluationResult;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * =================================================================================================
 * DECLARATIVE EJB TIMER BEAN: SUPPLY CHAIN MONITORING
 * -------------------------------------------------------------------------------------------------
 * Design Decision & Architectural Justification:
 * 1. @Singleton + @Startup:
 *    Ensures a single container-managed lifecycle instance initializes on application startup,
 *    preventing redundant competing automatic timer registrations across EJB instance pools.
 *
 * 2. Persistent Timer (persistent = true):
 *    Payara's EJB timer service persists timer schedules in its container timer repository.
 *    Guarantees that critical monitoring intervals survive application and server restarts.
 *
 * 3. Schedule Expression:
 *    @Schedule(hour = "*", minute = "* / 5", second = "0", persistent = true)
 *    Runs every 5 minutes in production.
 *
 * 4. Transaction Isolation:
 *    Delegates monitoring rule evaluations to SupplyChainMonitoringServiceBean coordinator,
 *    ensuring category-level transaction failure isolation across EJB proxy boundaries.
 * =================================================================================================
 */
@Singleton
@Startup
@TransactionManagement(TransactionManagementType.CONTAINER)
public class SupplyChainMonitoringTimerBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(SupplyChainMonitoringTimerBean.class.getName());

    @EJB
    private SupplyChainMonitoringServiceBean monitoringService;

    // Runtime observability statistics
    private volatile LocalDateTime lastMonitoringTime;
    private volatile long monitoringCycleCount = 0;
    private volatile String lastOverallStatus = "NOT_YET_EXECUTED";
    private volatile int lastSuccessfulCategories = 0;
    private volatile int lastFailedCategories = 0;
    private volatile int lastActiveAlertsCount = 0;
    private volatile int lastResolvedAlertsCount = 0;

    @PostConstruct
    public void init() {
        LOGGER.log(Level.INFO, "[SupplyChainMonitoringTimerBean] Initialized and active. Declarative @Schedule timer registered with Payara container.");
    }

    /**
     * Declarative / Automatic Timer Callback.
     * Triggered automatically by the Payara EJB Timer Service every 5 minutes.
     */
    @Schedule(hour = "*", minute = "*/5", second = "0", persistent = true, info = "DeclarativeSupplyChainMonitoringTimer")
    public void automaticMonitoringSchedule() {
        LOGGER.log(Level.INFO, "[SupplyChainMonitoringTimerBean] Automatic @Schedule timer triggered by Payara container.");
        runMonitoringCycle("AUTOMATIC_SCHEDULED_TIMER");
    }

    /**
     * Core monitoring cycle execution.
     * Can be invoked by the declarative @Schedule timer or manually via verification REST endpoint.
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public SupplyChainEvaluationResult runMonitoringCycle(String triggerSource) {
        monitoringCycleCount++;
        SupplyChainEvaluationResult result = monitoringService.evaluateSupplyChain(triggerSource);

        this.lastMonitoringTime = result.getExecutionTime();
        this.lastOverallStatus = result.getOverallStatus();
        this.lastSuccessfulCategories = result.getSuccessfulCategories();
        this.lastFailedCategories = result.getFailedCategories();
        this.lastActiveAlertsCount = result.getTotalActiveAlertsDetected();
        this.lastResolvedAlertsCount = result.getTotalAlertsResolved();

        LOGGER.log(Level.INFO, "[SupplyChainMonitoringTimerBean] Cycle #{0} complete. Overall Status: {1}",
                new Object[]{monitoringCycleCount, lastOverallStatus});

        return result;
    }

    public LocalDateTime getLastMonitoringTime() {
        return lastMonitoringTime;
    }

    public long getMonitoringCycleCount() {
        return monitoringCycleCount;
    }

    public String getLastOverallStatus() {
        return lastOverallStatus;
    }

    public int getLastSuccessfulCategories() {
        return lastSuccessfulCategories;
    }

    public int getLastFailedCategories() {
        return lastFailedCategories;
    }

    public int getLastActiveAlertsCount() {
        return lastActiveAlertsCount;
    }

    public int getLastResolvedAlertsCount() {
        return lastResolvedAlertsCount;
    }
}
