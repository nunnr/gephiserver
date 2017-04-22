package org.nunn.gephiserver.server.servlet.exception;

import java.util.concurrent.CancellationException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class CancellationExceptionMapper implements ExceptionMapper<CancellationException> {

	@Override
	public Response toResponse(CancellationException exception) {
		return Response.status(Response.Status.REQUEST_TIMEOUT).entity("Request operation cancelled.").type(MediaType.APPLICATION_JSON).build();
	}

}
