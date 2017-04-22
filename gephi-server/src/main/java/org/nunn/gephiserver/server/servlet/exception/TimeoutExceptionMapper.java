package org.nunn.gephiserver.server.servlet.exception;

import java.util.concurrent.TimeoutException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class TimeoutExceptionMapper implements ExceptionMapper<TimeoutException> {

	@Override
	public Response toResponse(TimeoutException exception) {
		return Response.status(Response.Status.REQUEST_TIMEOUT).entity("Request operation timed out.").type(MediaType.APPLICATION_JSON).build();
	}

}
