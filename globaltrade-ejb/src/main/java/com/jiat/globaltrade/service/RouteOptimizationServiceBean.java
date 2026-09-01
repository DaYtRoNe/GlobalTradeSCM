package com.jiat.globaltrade.service;

import com.jiat.globaltrade.entity.RouteOptimizationRecommendation;
import com.jiat.globaltrade.entity.RouteOption;
import com.jiat.globaltrade.entity.Shipment;
import com.jiat.globaltrade.entity.enums.ShipmentStatus;
import com.jiat.globaltrade.exception.ResourceNotFoundException;
import com.jiat.globaltrade.security.SecurityRoles;
import jakarta.annotation.Resource;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Core business service implementing the deterministic Route Optimization Engine.
 *
 * Algorithm Design & Weightings:
 * - Speed (Transit Hours): 45% (Delivery Speed)
 * - Cost (Estimated Freight Cost): 35% (Cost Efficiency)
 * - Risk (Operational Risk Score): 20% (Operational Reliability)
 *
 * All values are normalized in range [0.0, 1.0] across candidate routes matching the same corridor.
 */
@Stateless
@TransactionManagement(TransactionManagementType.CONTAINER)
@DeclareRoles({
        SecurityRoles.ADMIN,
        SecurityRoles.LOGISTICS_COORDINATOR,
        SecurityRoles.SYSTEM
})
public class RouteOptimizationServiceBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(RouteOptimizationServiceBean.class.getName());

    /** Transparent documented algorithm weighting constants */
    public static final BigDecimal WEIGHT_SPEED = new BigDecimal("0.45");
    public static final BigDecimal WEIGHT_COST = new BigDecimal("0.35");
    public static final BigDecimal WEIGHT_RISK = new BigDecimal("0.20");

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @Resource
    private SessionContext sessionContext;

    @EJB
    private AuditServiceBean auditService;

    /**
     * Evaluates candidate routes for a single active shipment and persists/updates the recommendation.
     */
    @PermitAll
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public RouteOptimizationRecommendation optimizeShipmentRoute(Long shipmentId, String triggerSource, String performedBy)
            throws ResourceNotFoundException {

        if (shipmentId == null) {
            throw new IllegalArgumentException("Shipment ID must not be null.");
        }

        Shipment shipment = em.find(Shipment.class, shipmentId);
        if (shipment == null) {
            throw new ResourceNotFoundException("Shipment", shipmentId);
        }

        // Active Shipment Eligibility Check
        if (shipment.getShipmentStatus() == ShipmentStatus.DELIVERED || shipment.getShipmentStatus() == ShipmentStatus.CANCELLED) {
            LOGGER.log(Level.INFO, "[RouteOptimizationServiceBean] Skipping non-active shipment #{0} (Status: {1})",
                    new Object[]{shipmentId, shipment.getShipmentStatus()});
            return null;
        }

        // Query active candidate routes matching shipment origin and destination
        List<RouteOption> candidates = em.createQuery(
                "SELECT r FROM RouteOption r WHERE r.origin = :origin AND r.destination = :dest AND r.active = TRUE",
                RouteOption.class)
                .setParameter("origin", shipment.getOrigin())
                .setParameter("dest", shipment.getDestination())
                .getResultList();

        if (candidates.isEmpty()) {
            LOGGER.log(Level.WARNING, "[RouteOptimizationServiceBean] No active route options found for corridor: {0} -> {1}",
                    new Object[]{shipment.getOrigin(), shipment.getDestination()});
            throw new ResourceNotFoundException("RouteOption (Corridor: " + shipment.getOrigin() + " -> " + shipment.getDestination() + ")", shipmentId);
        }

        // Calculate deterministic score for each candidate and select the best
        ScoredRoute bestScoredRoute = evaluateCandidateRoutes(candidates);
        RouteOption bestRoute = bestScoredRoute.getRoute();
        BigDecimal finalScore = bestScoredRoute.getScore();

        String rationale = String.format("Optimal corridor %s via %s (%s). Transit: %dh, Cost: $%s, Risk: %s, Score: %s",
                bestRoute.getRouteCode(), bestRoute.getCarrierName(), bestRoute.getTransportMode(),
                bestRoute.getEstimatedTransitHours(), bestRoute.getEstimatedCost(), bestRoute.getOperationalRiskScore(), finalScore);

        // Check if recommendation already exists for this shipment
        RouteOptimizationRecommendation existing = findRecommendationByShipmentIdInternal(shipmentId);

        if (existing == null) {
            // New Recommendation
            RouteOptimizationRecommendation rec = new RouteOptimizationRecommendation(
                    shipment,
                    bestRoute,
                    finalScore,
                    bestRoute.getEstimatedTransitHours(),
                    bestRoute.getEstimatedCost(),
                    bestRoute.getOperationalRiskScore(),
                    triggerSource != null ? triggerSource : "SYSTEM",
                    rationale
            );
            em.persist(rec);
            em.flush();

            LOGGER.log(Level.INFO, "[RouteOptimizationServiceBean] Route recommendation CREATED for Shipment #{0}: {1} (Score: {2})",
                    new Object[]{shipmentId, bestRoute.getRouteCode(), finalScore});

            auditService.logAction("ROUTE_RECOMMENDATION_CREATED", "Shipment", shipmentId, performedBy,
                    String.format("Recommended route %s for %s -> %s (Score: %s)",
                            bestRoute.getRouteCode(), shipment.getOrigin(), shipment.getDestination(), finalScore));

            return rec;
        }

        // Existing recommendation: check if the selected route changed
        Long previousRouteId = existing.getSelectedRoute() != null ? existing.getSelectedRoute().getId() : null;
        boolean routeChanged = previousRouteId == null || !previousRouteId.equals(bestRoute.getId());

        existing.setSelectedRoute(bestRoute);
        existing.setOptimizationScore(finalScore);
        existing.setTransitTimeHours(bestRoute.getEstimatedTransitHours());
        existing.setEstimatedCost(bestRoute.getEstimatedCost());
        existing.setRiskScore(bestRoute.getOperationalRiskScore());
        existing.setEvaluatedAt(LocalDateTime.now());
        existing.setEvaluationSource(triggerSource != null ? triggerSource : "SYSTEM");
        existing.setSummaryRationale(rationale);

        em.merge(existing);
        em.flush();

        if (routeChanged) {
            LOGGER.log(Level.INFO, "[RouteOptimizationServiceBean] Route recommendation CHANGED for Shipment #{0} -> {1} (Score: {2})",
                    new Object[]{shipmentId, bestRoute.getRouteCode(), finalScore});

            auditService.logAction("ROUTE_RECOMMENDATION_CHANGED", "Shipment", shipmentId, performedBy,
                    String.format("Updated route recommendation to %s (Score: %s)", bestRoute.getRouteCode(), finalScore));
        } else {
            LOGGER.log(Level.FINE, "[RouteOptimizationServiceBean] Route recommendation unchanged for Shipment #{0}: {1}",
                    new Object[]{shipmentId, bestRoute.getRouteCode()});
        }

        return existing;
    }

    /**
     * Executes transparent deterministic scoring across candidate routes for the same corridor.
     */
    public ScoredRoute evaluateCandidateRoutes(List<RouteOption> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalArgumentException("Candidate route list must not be empty.");
        }

        if (candidates.size() == 1) {
            RouteOption only = candidates.get(0);
            return new ScoredRoute(only, new BigDecimal("1.0000"));
        }

        // 1. Determine Min/Max boundaries for normalization
        int minHours = Integer.MAX_VALUE;
        int maxHours = Integer.MIN_VALUE;
        BigDecimal minCost = null;
        BigDecimal maxCost = null;
        BigDecimal minRisk = null;
        BigDecimal maxRisk = null;

        for (RouteOption r : candidates) {
            int h = r.getEstimatedTransitHours();
            if (h < minHours) minHours = h;
            if (h > maxHours) maxHours = h;

            BigDecimal c = r.getEstimatedCost();
            if (minCost == null || c.compareTo(minCost) < 0) minCost = c;
            if (maxCost == null || c.compareTo(maxCost) > 0) maxCost = c;

            BigDecimal rk = r.getOperationalRiskScore();
            if (minRisk == null || rk.compareTo(minRisk) < 0) minRisk = rk;
            if (maxRisk == null || rk.compareTo(maxRisk) > 0) maxRisk = rk;
        }

        // 2. Score each candidate
        BigDecimal bestScore = BigDecimal.valueOf(-1.0);
        RouteOption bestCandidate = null;

        for (RouteOption r : candidates) {
            // Speed Sub-Score (lower hours = higher score)
            BigDecimal speedSubScore;
            if (maxHours == minHours) {
                speedSubScore = BigDecimal.ONE;
            } else {
                double speedNorm = 1.0 - ((double) (r.getEstimatedTransitHours() - minHours) / (double) (maxHours - minHours));
                speedSubScore = BigDecimal.valueOf(speedNorm);
            }

            // Cost Sub-Score (lower cost = higher score)
            BigDecimal costSubScore;
            if (maxCost.compareTo(minCost) == 0) {
                costSubScore = BigDecimal.ONE;
            } else {
                BigDecimal costRange = maxCost.subtract(minCost);
                BigDecimal costDiff = r.getEstimatedCost().subtract(minCost);
                costSubScore = BigDecimal.ONE.subtract(costDiff.divide(costRange, 6, RoundingMode.HALF_UP));
            }

            // Risk Sub-Score (lower risk = higher score)
            BigDecimal riskSubScore;
            if (maxRisk.compareTo(minRisk) == 0) {
                riskSubScore = BigDecimal.ONE;
            } else {
                BigDecimal riskRange = maxRisk.subtract(minRisk);
                BigDecimal riskDiff = r.getOperationalRiskScore().subtract(minRisk);
                riskSubScore = BigDecimal.ONE.subtract(riskDiff.divide(riskRange, 6, RoundingMode.HALF_UP));
            }

            // Composite Weighted Score
            BigDecimal composite = speedSubScore.multiply(WEIGHT_SPEED)
                    .add(costSubScore.multiply(WEIGHT_COST))
                    .add(riskSubScore.multiply(WEIGHT_RISK))
                    .setScale(4, RoundingMode.HALF_UP);

            if (bestCandidate == null || isBetterCandidate(r, composite, bestCandidate, bestScore)) {
                bestScore = composite;
                bestCandidate = r;
            }
        }

        return new ScoredRoute(bestCandidate, bestScore);
    }

    /**
     * Deterministic comparison with tie-breaking (Score > Cost > Transit Hours > ID).
     */
    private boolean isBetterCandidate(RouteOption candidate, BigDecimal candidateScore,
                                      RouteOption currentBest, BigDecimal currentBestScore) {
        int scoreCmp = candidateScore.compareTo(currentBestScore);
        if (scoreCmp != 0) {
            return scoreCmp > 0;
        }

        // Tie-breaker 1: Lowest cost
        int costCmp = candidate.getEstimatedCost().compareTo(currentBest.getEstimatedCost());
        if (costCmp != 0) {
            return costCmp < 0;
        }

        // Tie-breaker 2: Lowest transit hours
        int hoursCmp = candidate.getEstimatedTransitHours().compareTo(currentBest.getEstimatedTransitHours());
        if (hoursCmp != 0) {
            return hoursCmp < 0;
        }

        // Tie-breaker 3: Deterministic ID order
        return candidate.getId() < currentBest.getId();
    }

    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.LOGISTICS_COORDINATOR})
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<RouteOption> findRouteOptions(String origin, String destination, Boolean activeOnly) {
        StringBuilder jpql = new StringBuilder("SELECT r FROM RouteOption r WHERE 1=1");
        if (origin != null && !origin.isBlank()) {
            jpql.append(" AND LOWER(r.origin) = LOWER(:origin)");
        }
        if (destination != null && !destination.isBlank()) {
            jpql.append(" AND LOWER(r.destination) = LOWER(:destination)");
        }
        if (activeOnly != null && activeOnly) {
            jpql.append(" AND r.active = TRUE");
        }
        jpql.append(" ORDER BY r.origin, r.destination, r.estimatedCost");

        TypedQuery<RouteOption> query = em.createQuery(jpql.toString(), RouteOption.class);
        if (origin != null && !origin.isBlank()) {
            query.setParameter("origin", origin.trim());
        }
        if (destination != null && !destination.isBlank()) {
            query.setParameter("destination", destination.trim());
        }
        return query.getResultList();
    }

    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.LOGISTICS_COORDINATOR})
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<RouteOptimizationRecommendation> findAllRecommendations() {
        return em.createQuery(
                "SELECT r FROM RouteOptimizationRecommendation r JOIN FETCH r.shipment JOIN FETCH r.selectedRoute ORDER BY r.evaluatedAt DESC",
                RouteOptimizationRecommendation.class)
                .getResultList();
    }

    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.LOGISTICS_COORDINATOR})
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public RouteOptimizationRecommendation findRecommendationByShipmentId(Long shipmentId) throws ResourceNotFoundException {
        RouteOptimizationRecommendation rec = findRecommendationByShipmentIdInternal(shipmentId);
        if (rec == null) {
            throw new ResourceNotFoundException("RouteOptimizationRecommendation (Shipment #" + shipmentId + ")", shipmentId);
        }
        return rec;
    }

    private RouteOptimizationRecommendation findRecommendationByShipmentIdInternal(Long shipmentId) {
        List<RouteOptimizationRecommendation> list = em.createQuery(
                "SELECT r FROM RouteOptimizationRecommendation r JOIN FETCH r.shipment JOIN FETCH r.selectedRoute WHERE r.shipment.id = :shipmentId",
                RouteOptimizationRecommendation.class)
                .setParameter("shipmentId", shipmentId)
                .getResultList();
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * Internal container holding a candidate route and its calculated composite score.
     */
    public static class ScoredRoute implements Serializable {
        private static final long serialVersionUID = 1L;

        private final RouteOption route;
        private final BigDecimal score;

        public ScoredRoute(RouteOption route, BigDecimal score) {
            this.route = route;
            this.score = score;
        }

        public RouteOption getRoute() {
            return route;
        }

        public BigDecimal getScore() {
            return score;
        }
    }
}
