package org.nunn.gephiserver.servlet;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.nunn.gephiserver.graphing.GraphBuilder;
import org.nunn.gephiserver.graphing.GraphOutput;
import org.nunn.gephiserver.system.MimeType;
import org.nunn.gephiserver.system.StringBuilderWriter;

//Sets the path to base URL + /graph
@Path("/graph")
public class Graph {
	
	@GET
	@Path("/stdSvg")
	@Produces({MimeType.APPLICATION_SVG_XML, MimeType.TEXT_HTML})
	public String stdSvg(@QueryParam("graphId") Integer graphId) throws Exception {
		Map<String, Object> extraParam = Collections.<String, Object>emptyMap();
		GraphOutput<StringBuilderWriter> result = GraphBuilder.INSTANCE.doGraph(GraphBuilder.INSTANCE.logicStd, GraphBuilder.INSTANCE.layoutStd, GraphBuilder.INSTANCE.exporterSvg, graphId, extraParam);
		return result.getOutput().toString();
	}
	
	@GET
	@Path("/rootySvg")
	@Produces({MimeType.APPLICATION_SVG_XML, MimeType.TEXT_HTML})
	public String rootySvg(@QueryParam("graphId") Integer graphId, @QueryParam("rootNodeId") int rootNodeId) throws Exception {
		Map<String, Object> extraParam = Collections.<String, Object>singletonMap("rootNodeId", rootNodeId);
		GraphOutput<StringBuilderWriter> result = GraphBuilder.INSTANCE.doGraph(GraphBuilder.INSTANCE.logicRoot, GraphBuilder.INSTANCE.layoutStd, GraphBuilder.INSTANCE.exporterSvg, graphId, extraParam);
		return result.getOutput().toString();
	}
	
	@GET
	@Path("/stdSvgAsync")
	@Produces({MimeType.TEXT_PLAIN})
	public String stdSvgAsync(@QueryParam("graphId") Integer graphId) {
		Map<String, Object> extraParam = Collections.<String, Object>emptyMap();
		return GraphBuilder.INSTANCE.doGraphAsync(GraphBuilder.INSTANCE.logicStd, GraphBuilder.INSTANCE.layoutStd, GraphBuilder.INSTANCE.exporterSvg, graphId, extraParam);
	}
	
	@GET
	@Path("/rootySvgAsync")
	@Produces({MimeType.TEXT_PLAIN})
	public String rootySvgAsync(@QueryParam("graphId") Integer graphId, @QueryParam("rootNodeId") int rootNodeId) {
		Map<String, Object> extraParam = Collections.<String, Object>singletonMap("rootNodeId", rootNodeId);
		return GraphBuilder.INSTANCE.doGraphAsync(GraphBuilder.INSTANCE.logicRoot, GraphBuilder.INSTANCE.layoutStd, GraphBuilder.INSTANCE.exporterSvg, graphId, extraParam);
	}
	
	@GET
	@Path("/getSvgAsyncResult")
	@Produces({MimeType.APPLICATION_SVG_XML, MimeType.TEXT_HTML})
	public String getSvgAsyncResult(@QueryParam("uuid") String uuid) throws Exception {
		GraphOutput<StringBuilderWriter> result = GraphBuilder.INSTANCE.getAsyncResult(uuid);
		return result != null ? result.getOutput().toString() : null;
	}
	
	@GET
	@Path("/stdPdf")
	@Produces({MimeType.APPLICATION_PDF})
	public byte[] stdPdf(@QueryParam("graphId") Integer graphId) throws Exception {
		Map<String, Object> extraParam = Collections.<String, Object>emptyMap();
		GraphOutput<ByteArrayOutputStream> result = GraphBuilder.INSTANCE.doGraph(GraphBuilder.INSTANCE.logicStd, GraphBuilder.INSTANCE.layoutStd, GraphBuilder.INSTANCE.exporterPdf, graphId, extraParam);;
		return result.getOutput().toByteArray();
	}
	
	@GET
	@Path("/rootyPdf")
	@Produces({MimeType.APPLICATION_PDF})
	public byte[] rootyPdf(@QueryParam("graphId") Integer graphId, @QueryParam("rootNodeId") int rootNodeId) throws Exception {
		Map<String, Object> extraParam = Collections.<String, Object>singletonMap("rootNodeId", rootNodeId);
		GraphOutput<ByteArrayOutputStream> result = GraphBuilder.INSTANCE.doGraph(GraphBuilder.INSTANCE.logicRoot, GraphBuilder.INSTANCE.layoutStd, GraphBuilder.INSTANCE.exporterPdf, graphId, extraParam);;
		return result.getOutput().toByteArray();
	}
	
	@GET
	@Path("/stdPdfAsync")
	@Produces({MimeType.TEXT_PLAIN})
	public String stdPdfAsync(@QueryParam("graphId") Integer graphId) {
		Map<String, Object> extraParam = Collections.<String, Object>emptyMap();
		return GraphBuilder.INSTANCE.doGraphAsync(GraphBuilder.INSTANCE.logicStd, GraphBuilder.INSTANCE.layoutStd, GraphBuilder.INSTANCE.exporterPdf, graphId, extraParam);
	}
	
	@GET
	@Path("/rootyPdfAsync")
	@Produces({MimeType.TEXT_PLAIN})
	public String rootyPdfAsync(@QueryParam("graphId") Integer graphId, @QueryParam("rootNodeId") int rootNodeId) {
		Map<String, Object> extraParam = Collections.<String, Object>singletonMap("rootNodeId", rootNodeId);
		return GraphBuilder.INSTANCE.doGraphAsync(GraphBuilder.INSTANCE.logicRoot, GraphBuilder.INSTANCE.layoutStd, GraphBuilder.INSTANCE.exporterPdf, graphId, extraParam);
	}
	
	@GET
	@Path("/getPdfAsyncResult")
	@Produces({MimeType.APPLICATION_PDF})
	public byte[] getPdfAsyncResult(@QueryParam("uuid") String uuid) throws Exception {
		GraphOutput<ByteArrayOutputStream> result = GraphBuilder.INSTANCE.getAsyncResult(uuid);
		return result != null ? result.getOutput().toByteArray() : null;
	}
	
}
