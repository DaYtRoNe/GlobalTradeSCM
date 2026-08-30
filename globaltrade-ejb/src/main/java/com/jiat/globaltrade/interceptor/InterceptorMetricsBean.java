package com.jiat.globaltrade.interceptor;

import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.Singleton;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe Singleton EJB maintaining runtime in-memory performance metrics
 * recorded by PerformanceMonitoringInterceptor across business service invocations.
 *
 * Configured with NOT_SUPPORTED to guarantee that in-memory metrics operations
 * execute without participating in any business transaction context and can never
 * fail due to a rollback-marked transaction.
 */
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class InterceptorMetricsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private final AtomicLong totalInvocations = new AtomicLong(0);
    private final AtomicLong totalExecutionNanos = new AtomicLong(0);
    private final AtomicLong maxExecutionNanos = new AtomicLong(0);
    private final AtomicLong lastExecutionNanos = new AtomicLong(0);
    private final AtomicReference<String> lastMethodName = new AtomicReference<>("None");

    public void recordInvocation(String methodName, long durationNanos) {
        totalInvocations.incrementAndGet();
        totalExecutionNanos.addAndGet(durationNanos);
        lastExecutionNanos.set(durationNanos);
        lastMethodName.set(methodName != null ? methodName : "Unknown");

        // Update maximum execution time atomically
        maxExecutionNanos.accumulateAndGet(durationNanos, Math::max);
    }

    public long getTotalInvocations() {
        return totalInvocations.get();
    }

    public long getAverageExecutionMicros() {
        long count = totalInvocations.get();
        if (count == 0) {
            return 0;
        }
        return (totalExecutionNanos.get() / count) / 1000L;
    }

    public long getMaxExecutionMicros() {
        return maxExecutionNanos.get() / 1000L;
    }

    public long getLastExecutionMicros() {
        return lastExecutionNanos.get() / 1000L;
    }

    public String getLastMethodName() {
        return lastMethodName.get();
    }

    public void reset() {
        totalInvocations.set(0);
        totalExecutionNanos.set(0);
        maxExecutionNanos.set(0);
        lastExecutionNanos.set(0);
        lastMethodName.set("None");
    }
}
