package org.nunn.gephiserver.server.servlet.exception;

import java.util.concurrent.RejectedExecutionException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class RejectedExecutionExceptionMapper implements ExceptionMapper<RejectedExecutionException> {

	@Override
	public Response toResponse(RejectedExecutionException exception) {
		return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("Server too busy.").type(MediaType.APPLICATION_JSON).build();
	}

}
