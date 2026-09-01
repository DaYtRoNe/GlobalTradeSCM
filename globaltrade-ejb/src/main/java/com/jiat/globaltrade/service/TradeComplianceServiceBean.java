package com.jiat.globaltrade.service;

import com.jiat.globaltrade.entity.CustomsDocument;
import com.jiat.globaltrade.entity.Shipment;
import com.jiat.globaltrade.entity.TradeAgreementRule;
import com.jiat.globaltrade.entity.enums.CustomsDocumentStatus;
import com.jiat.globaltrade.entity.enums.CustomsDocumentType;
import com.jiat.globaltrade.exception.ResourceNotFoundException;
import com.jiat.globaltrade.security.SecurityRoles;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for International Trade Agreement & Regulatory Compliance validation.
 * Verifies bilateral/multilateral trade agreement document requirements across origin and destination corridors.
 */
@Stateless
@TransactionManagement(TransactionManagementType.CONTAINER)
@DeclareRoles({
        SecurityRoles.ADMIN,
        SecurityRoles.LOGISTICS_COORDINATOR,
        SecurityRoles.CUSTOMS_AGENT
})
public class TradeComplianceServiceBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(TradeComplianceServiceBean.class.getName());

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @EJB
    private ShipmentServiceBean shipmentService;

    @EJB
    private CustomsServiceBean customsService;

    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.LOGISTICS_COORDINATOR, SecurityRoles.CUSTOMS_AGENT})
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<TradeAgreementRule> findAllAgreementRules() {
        return em.createQuery("SELECT r FROM TradeAgreementRule r WHERE r.active = true ORDER BY r.agreementCode ASC",
                TradeAgreementRule.class).getResultList();
    }

    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.LOGISTICS_COORDINATOR, SecurityRoles.CUSTOMS_AGENT})
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<TradeAgreementRule> findRulesByCorridor(String origin, String destination) {
        if (origin == null || destination == null) {
            return List.of();
        }
        return em.createQuery(
                "SELECT r FROM TradeAgreementRule r WHERE r.active = true " +
                        "AND (:origin LIKE CONCAT('%', r.originCountry, '%') OR r.originCountry LIKE CONCAT('%', :origin, '%')) " +
                        "AND (:destination LIKE CONCAT('%', r.destinationCountry, '%') OR r.destinationCountry LIKE CONCAT('%', :destination, '%'))",
                TradeAgreementRule.class)
                .setParameter("origin", origin)
                .setParameter("destination", destination)
                .getResultList();
    }

    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.LOGISTICS_COORDINATOR, SecurityRoles.CUSTOMS_AGENT})
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public TradeComplianceEvaluationResult evaluateTradeAgreementCompliance(Long shipmentId) throws ResourceNotFoundException {
        if (shipmentId == null) {
            throw new IllegalArgumentException("Shipment ID must not be null.");
        }

        Shipment shipment = shipmentService.findShipmentById(shipmentId);
        if (shipment == null) {
            throw new ResourceNotFoundException("Shipment", shipmentId);
        }

        List<TradeAgreementRule> applicableRules = findRulesByCorridor(shipment.getOrigin(), shipment.getDestination());
        List<CustomsDocument> documents = em.createQuery(
                "SELECT d FROM CustomsDocument d WHERE d.shipment.id = :shipmentId", CustomsDocument.class)
                .setParameter("shipmentId", shipmentId)
                .getResultList();

        Set<CustomsDocumentType> filedValidDocTypes = new HashSet<>();
        for (CustomsDocument doc : documents) {
            if (doc.getStatus() == CustomsDocumentStatus.APPROVED || doc.getStatus() == CustomsDocumentStatus.SUBMITTED) {
                filedValidDocTypes.add(doc.getDocumentType());
            }
        }

        List<String> evaluatedAgreements = new ArrayList<>();
        List<String> missingDocTypes = new ArrayList<>();
        List<String> satisfiedDocTypes = new ArrayList<>();

        for (TradeAgreementRule rule : applicableRules) {
            evaluatedAgreements.add(rule.getAgreementCode() + " (" + rule.getAgreementName() + ")");
            if (filedValidDocTypes.contains(rule.getDocumentTypeRequired())) {
                satisfiedDocTypes.add(rule.getDocumentTypeRequired().name() + " for " + rule.getAgreementCode());
            } else {
                missingDocTypes.add(rule.getDocumentTypeRequired().name() + " (Required by " + rule.getAgreementCode() + ")");
            }
        }

        boolean compliant = missingDocTypes.isEmpty();
        String summaryRationale;
        if (applicableRules.isEmpty()) {
            summaryRationale = "No specific bilateral trade agreement rules configured for corridor " +
                    shipment.getOrigin() + " -> " + shipment.getDestination() + ". Standard international customs clearance applies.";
        } else if (compliant) {
            summaryRationale = String.format("Shipment fully compliant with %d applicable trade agreements (%s). All required documentation present.",
                    applicableRules.size(), String.join(", ", evaluatedAgreements));
        } else {
            summaryRationale = String.format("Shipment has trade compliance deficiencies: %d mandatory documents missing (%s).",
                    missingDocTypes.size(), String.join(", ", missingDocTypes));
        }

        LOGGER.log(Level.INFO, "[TradeComplianceServiceBean] Evaluated shipment {0} ({1} -> {2}): Compliant={3}",
                new Object[]{shipment.getTrackingNumber(), shipment.getOrigin(), shipment.getDestination(), compliant});

        return new TradeComplianceEvaluationResult(
                shipment.getId(),
                shipment.getTrackingNumber(),
                shipment.getOrigin(),
                shipment.getDestination(),
                compliant,
                evaluatedAgreements,
                satisfiedDocTypes,
                missingDocTypes,
                summaryRationale
        );
    }

    public record TradeComplianceEvaluationResult(
            Long shipmentId,
            String trackingNumber,
            String origin,
            String destination,
            boolean compliant,
            List<String> applicableAgreements,
            List<String> satisfiedDocuments,
            List<String> missingDocuments,
            String rationale
    ) implements Serializable {}
}
