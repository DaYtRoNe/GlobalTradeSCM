package com.jiat.globaltrade.web.mapper;

import com.jiat.globaltrade.web.dto.ApiErrorResponse;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JAX-RS ExceptionMapper for WebApplicationException and its subclasses
 * (NotFoundException, BadRequestException, NotAllowedException, etc.).
 * Preserves standard HTTP semantics and formats responses as clean, structured JSON.
 */
@Provider
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {

    private static final Logger LOGGER = Logger.getLogger(WebApplicationExceptionMapper.class.getName());

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(WebApplicationException exception) {
        String path = uriInfo != null ? uriInfo.getPath() : "/api";
        int statusCode = exception.getResponse() != null ? exception.getResponse().getStatus() : Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
        
        Response.Status responseStatus = Response.Status.fromStatusCode(statusCode);
        String statusName = responseStatus != null ? responseStatus.name() : "HTTP_" + statusCode;
        String message = exception.getMessage() != null && !exception.getMessage().isEmpty()
                ? exception.getMessage()
                : (responseStatus != null ? responseStatus.getReasonPhrase() : "HTTP error " + statusCode);

        LOGGER.log(Level.INFO, "[WebApplicationExceptionMapper] HTTP {0} ({1}) at {2}: {3}",
                new Object[]{statusCode, statusName, path, message});

        ApiErrorResponse error = new ApiErrorResponse(
                statusName,
                "HTTP_" + statusCode,
                message,
                path
        );

        return Response.status(statusCode)
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build();
    }
}
