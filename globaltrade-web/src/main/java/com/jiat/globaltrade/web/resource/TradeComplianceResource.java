package com.jiat.globaltrade.web.resource;

import com.jiat.globaltrade.entity.TradeAgreementRule;
import com.jiat.globaltrade.exception.ResourceNotFoundException;
import com.jiat.globaltrade.security.SecurityRoles;
import com.jiat.globaltrade.service.TradeComplianceServiceBean;
import com.jiat.globaltrade.web.dto.TradeAgreementRuleResponse;
import com.jiat.globaltrade.web.dto.TradeComplianceResponse;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/trade-compliance")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@DeclareRoles({
        SecurityRoles.ADMIN,
        SecurityRoles.LOGISTICS_COORDINATOR,
        SecurityRoles.CUSTOMS_AGENT
})
public class TradeComplianceResource {

    @EJB
    private TradeComplianceServiceBean tradeComplianceService;

    @GET
    @Path("/shipment/{shipmentId}")
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.LOGISTICS_COORDINATOR, SecurityRoles.CUSTOMS_AGENT})
    public Response evaluateShipmentTradeCompliance(@PathParam("shipmentId") Long shipmentId) {
        try {
            TradeComplianceServiceBean.TradeComplianceEvaluationResult result =
                    tradeComplianceService.evaluateTradeAgreementCompliance(shipmentId);

            TradeComplianceResponse response = new TradeComplianceResponse(
                    result.shipmentId(),
                    result.trackingNumber(),
                    result.origin(),
                    result.destination(),
                    result.compliant(),
                    result.applicableAgreements(),
                    result.satisfiedDocuments(),
                    result.missingDocuments(),
                    result.rationale()
            );

            return Response.ok(response).build();
        } catch (ResourceNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Shipment not found: " + shipmentId + "\"}")
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    @GET
    @Path("/rules")
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.LOGISTICS_COORDINATOR, SecurityRoles.CUSTOMS_AGENT})
    public Response listAllTradeAgreementRules() {
        List<TradeAgreementRule> rules = tradeComplianceService.findAllAgreementRules();
        List<TradeAgreementRuleResponse> responseList = rules.stream()
                .map(r -> new TradeAgreementRuleResponse(
                        r.getId(),
                        r.getAgreementCode(),
                        r.getAgreementName(),
                        r.getOriginCountry(),
                        r.getDestinationCountry(),
                        r.getDocumentTypeRequired() != null ? r.getDocumentTypeRequired().name() : null,
                        r.isActive(),
                        r.getDescription()
                ))
                .toList();

        return Response.ok(responseList).build();
    }
}
