package com.jiat.globaltrade.timer;

import com.jiat.globaltrade.service.RouteOptimizationCoordinatorBean;
import com.jiat.globaltrade.service.RouteOptimizationCoordinatorBean.RouteOptimizationBatchSummary;
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
 * DECLARATIVE EJB TIMER BEAN: AUTOMATED ROUTE OPTIMIZATION
 * -------------------------------------------------------------------------------------------------
 * Design Decision & Architectural Justification:
 * 1. @Singleton + @Startup:
 *    Ensures a single container-managed lifecycle instance initializes on startup,
 *    preventing duplicate competing automatic route timer registrations.
 *
 * 2. Persistent Timer (persistent = true):
 *    Payara's EJB timer service registers and persists the timer schedule in its timer store,
 *    guaranteeing that 10-minute route optimization intervals survive server restarts.
 *
 * 3. Schedule Expression:
 *    @Schedule(hour = "*", minute = "* / 10", second = "0", persistent = true)
 *    Runs automatically every 10 minutes in production.
 *
 * 4. Transaction Isolation:
 *    Delegates route evaluations to RouteOptimizationCoordinatorBean, ensuring that each
 *    shipment is scored and committed in an independent REQUIRES_NEW transaction.
 * =================================================================================================
 */
@Singleton
@Startup
@TransactionManagement(TransactionManagementType.CONTAINER)
public class RouteOptimizationTimerBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(RouteOptimizationTimerBean.class.getName());

    @EJB
    private RouteOptimizationCoordinatorBean coordinatorBean;

    // Observability & Telemetry statistics
    private volatile LocalDateTime lastOptimizationTime;
    private volatile long cycleCount = 0;
    private volatile String lastOverallStatus = "NOT_YET_EXECUTED";
    private volatile int lastTotalEvaluated = 0;
    private volatile int lastSuccessfulOptimizations = 0;
    private volatile int lastFailedOptimizations = 0;

    @PostConstruct
    public void init() {
        LOGGER.log(Level.INFO, "[RouteOptimizationTimerBean] Initialized. Declarative @Schedule timer (every 10 min) registered with Payara container.");
    }

    /**
     * Automatic Timer Callback triggered every 10 minutes by Payara container.
     */
    @Schedule(hour = "*", minute = "*/10", second = "0", persistent = true, info = "DeclarativeRouteOptimizationTimer")
    public void automaticOptimizationSchedule() {
        LOGGER.log(Level.INFO, "[RouteOptimizationTimerBean] Automatic @Schedule timer triggered.");
        runOptimizationCycle("AUTOMATIC_SCHEDULED_TIMER", "SYSTEM_TIMER");
    }

    /**
     * Executes route optimization cycle manually or via scheduled trigger.
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public RouteOptimizationBatchSummary runOptimizationCycle(String triggerSource, String performedBy) {
        cycleCount++;
        RouteOptimizationBatchSummary summary = coordinatorBean.optimizeAllActiveShipments(triggerSource, performedBy);

        this.lastOptimizationTime = summary.getExecutionTime();
        this.lastOverallStatus = summary.getOverallStatus();
        this.lastTotalEvaluated = summary.getTotalShipmentsEvaluated();
        this.lastSuccessfulOptimizations = summary.getSuccessfulOptimizations();
        this.lastFailedOptimizations = summary.getFailedOptimizations();

        LOGGER.log(Level.INFO, "[RouteOptimizationTimerBean] Cycle #{0} complete. Overall Status: {1}",
                new Object[]{cycleCount, lastOverallStatus});

        return summary;
    }

    public LocalDateTime getLastOptimizationTime() {
        return lastOptimizationTime;
    }

    public long getCycleCount() {
        return cycleCount;
    }

    public String getLastOverallStatus() {
        return lastOverallStatus;
    }

    public int getLastTotalEvaluated() {
        return lastTotalEvaluated;
    }

    public int getLastSuccessfulOptimizations() {
        return lastSuccessfulOptimizations;
    }

    public int getLastFailedOptimizations() {
        return lastFailedOptimizations;
    }
}
