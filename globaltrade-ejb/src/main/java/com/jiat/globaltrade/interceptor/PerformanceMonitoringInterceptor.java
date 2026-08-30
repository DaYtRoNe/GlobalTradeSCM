package com.jiat.globaltrade.interceptor;

import jakarta.ejb.EJB;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cross-cutting performance monitoring interceptor.
 * Accurately measures method execution duration in nanoseconds using System.nanoTime(),
 * logs diagnostic metrics, and records runtime statistics in InterceptorMetricsBean.
 */
public class PerformanceMonitoringInterceptor implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(PerformanceMonitoringInterceptor.class.getName());

    @EJB
    private InterceptorMetricsBean metricsBean;

    @AroundInvoke
    public Object monitorPerformance(InvocationContext context) throws Exception {
        long startNanos = System.nanoTime();
        String targetClass = context.getTarget() != null ? context.getTarget().getClass().getSimpleName() : "UnknownClass";
        String methodName = context.getMethod() != null ? context.getMethod().getName() : "unknownMethod";
        String methodSignature = targetClass + "." + methodName;

        LOGGER.log(Level.INFO, "[PerformanceMonitoringInterceptor] [START] Entering {0}", methodSignature);

        try {
            return context.proceed();
        } finally {
            long durationNanos = System.nanoTime() - startNanos;
            long durationMicros = durationNanos / 1000L;

            LOGGER.log(Level.INFO, "[PerformanceMonitoringInterceptor] [END] {0} executed in {1} microseconds ({2} ns)",
                    new Object[]{methodSignature, durationMicros, durationNanos});

            if (metricsBean != null) {
                try {
                    metricsBean.recordInvocation(methodSignature, durationNanos);
                } catch (Exception metricsError) {
                    LOGGER.log(Level.WARNING, "[PerformanceMonitoringInterceptor] Unable to record interceptor performance metrics for {0}: {1}",
                            new Object[]{methodSignature, metricsError.getMessage()});
                }
            }
        }
    }
}
