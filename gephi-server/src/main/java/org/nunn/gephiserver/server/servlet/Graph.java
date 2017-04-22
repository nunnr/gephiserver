package org.nunn.gephiserver.server.servlet;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.Map;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.nunn.gephiserver.server.graphing.GraphBuilder;
import org.nunn.gephiserver.server.graphing.GraphOutput;
import org.nunn.gephiserver.server.system.MimeType;
import org.nunn.gephiserver.server.system.StringBuilderWriter;

@Path("/graph") //Sets the path to base URL + /graph
public class Graph {
	
	@GET
	@Path("/list")
	@Produces({MimeType.APPLICATION_JSON})
	public Map<Integer, String> list() {
		return GraphBuilder.INSTANCE.listGraphs();
	}
	
	@POST
	@Path("/stdSvg")
	@Produces({MimeType.APPLICATION_SVG_XML, MimeType.TEXT_HTML})
	public String stdSvg(@FormParam("graphId") Integer graphId) throws Exception {
		Map<String, Object> extraParam = Collections.<String, Object>emptyMap();
		GraphOutput<StringBuilderWriter> result = GraphBuilder.INSTANCE.doGraph(GraphBuilder.INSTANCE.logicStd, GraphBuilder.INSTANCE.layoutStd, GraphBuilder.INSTANCE.exporterSvg, graphId, extraParam);
		return result.output.toString();
	}
	
	@POST
	@Path("/rootySvg")
	@Produces({MimeType.APPLICATION_SVG_XML, MimeType.TEXT_HTML})
	public String rootySvg(@FormParam("graphId") Integer graphId, @FormParam("rootNodeId") int rootNodeId) throws Exception {
		Map<String, Object> extraParam = Collections.<String, Object>singletonMap("rootNodeId", rootNodeId);
		GraphOutput<StringBuilderWriter> result = GraphBuilder.INSTANCE.doGraph(GraphBuilder.INSTANCE.logicRoot, GraphBuilder.INSTANCE.layoutStd, GraphBuilder.INSTANCE.exporterSvg, graphId, extraParam);
		return result.output.toString();
	}
	
	@POST
	@Path("/stdSvgAsync")
	@Produces({MimeType.TEXT_PLAIN})
	public String stdSvgAsync(@FormParam("graphId") Integer graphId) {
		Map<String, Object> extraParam = Collections.<String, Object>emptyMap();
		return GraphBuilder.INSTANCE.doGraphAsync(GraphBuilder.INSTANCE.logicStd, GraphBuilder.INSTANCE.layoutStd, GraphBuilder.INSTANCE.exporterSvg, graphId, extraParam);
	}
	
	@POST
	@Path("/rootySvgAsync")
	@Produces({MimeType.TEXT_PLAIN})
	public String rootySvgAsync(@FormParam("graphId") Integer graphId, @FormParam("rootNodeId") int rootNodeId) {
		Map<String, Object> extraParam = Collections.<String, Object>singletonMap("rootNodeId", rootNodeId);
		return GraphBuilder.INSTANCE.doGraphAsync(GraphBuilder.INSTANCE.logicRoot, GraphBuilder.INSTANCE.layoutStd, GraphBuilder.INSTANCE.exporterSvg, graphId, extraParam);
	}
	
	@POST
	@Path("/getSvgAsyncResult")
	@Produces({MimeType.APPLICATION_SVG_XML, MimeType.TEXT_HTML})
	public String getSvgAsyncResult(@FormParam("uuid") String uuid) throws Exception {
		GraphOutput<StringBuilderWriter> result = GraphBuilder.INSTANCE.getAsyncResult(uuid);
		return result != null ? result.output.toString() : null;
	}
	
	@POST
	@Path("/stdPdf")
	@Produces({MimeType.APPLICATION_PDF})
	public byte[] stdPdf(@FormParam("graphId") Integer graphId) throws Exception {
		Map<String, Object> extraParam = Collections.<String, Object>emptyMap();
		GraphOutput<ByteArrayOutputStream> result = GraphBuilder.INSTANCE.doGraph(GraphBuilder.INSTANCE.logicStd, GraphBuilder.INSTANCE.layoutStd, GraphBuilder.INSTANCE.exporterPdf, graphId, extraParam);;
		return result.output.toByteArray();
	}
	
	@POST
	@Path("/rootyPdf")
	@Produces({MimeType.APPLICATION_PDF})
	public byte[] rootyPdf(@FormParam("graphId") Integer graphId, @FormParam("rootNodeId") int rootNodeId) throws Exception {
		Map<String, Object> extraParam = Collections.<String, Object>singletonMap("rootNodeId", rootNodeId);
		GraphOutput<ByteArrayOutputStream> result = GraphBuilder.INSTANCE.doGraph(GraphBuilder.INSTANCE.logicRoot, GraphBuilder.INSTANCE.layoutStd, GraphBuilder.INSTANCE.exporterPdf, graphId, extraParam);;
		return result.output.toByteArray();
	}
	
	@POST
	@Path("/stdPdfAsync")
	@Produces({MimeType.TEXT_PLAIN})
	public String stdPdfAsync(@FormParam("graphId") Integer graphId) {
		Map<String, Object> extraParam = Collections.<String, Object>emptyMap();
		return GraphBuilder.INSTANCE.doGraphAsync(GraphBuilder.INSTANCE.logicStd, GraphBuilder.INSTANCE.layoutStd, GraphBuilder.INSTANCE.exporterPdf, graphId, extraParam);
	}
	
	@POST
	@Path("/rootyPdfAsync")
	@Produces({MimeType.TEXT_PLAIN})
	public String rootyPdfAsync(@FormParam("graphId") Integer graphId, @FormParam("rootNodeId") int rootNodeId) {
		Map<String, Object> extraParam = Collections.<String, Object>singletonMap("rootNodeId", rootNodeId);
		return GraphBuilder.INSTANCE.doGraphAsync(GraphBuilder.INSTANCE.logicRoot, GraphBuilder.INSTANCE.layoutStd, GraphBuilder.INSTANCE.exporterPdf, graphId, extraParam);
	}
	
	@POST
	@Path("/getPdfAsyncResult")
	@Produces({MimeType.APPLICATION_PDF})
	public byte[] getPdfAsyncResult(@FormParam("uuid") String uuid) throws Exception {
		GraphOutput<ByteArrayOutputStream> result = GraphBuilder.INSTANCE.getAsyncResult(uuid);
		return result != null ? result.output.toByteArray() : null;
	}
	
}