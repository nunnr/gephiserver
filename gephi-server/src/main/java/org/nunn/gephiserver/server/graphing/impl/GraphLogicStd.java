package org.nunn.gephiserver.server.graphing.impl;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gephi.io.importer.api.Container;
import org.gephi.io.importer.api.ContainerLoader;
import org.gephi.io.importer.api.EdgeDirectionDefault;
import org.gephi.io.importer.api.EdgeDraft;
import org.gephi.io.importer.api.ElementDraft;
import org.gephi.io.importer.api.NodeDraft;
import org.gephi.io.importer.api.Report;
import org.nunn.gephiserver.server.graphing.GraphDataSource;
import org.nunn.gephiserver.server.graphing.GraphLogic;
import org.openide.util.Lookup;

public class GraphLogicStd implements GraphLogic {
	
	private static final Logger LOGGER = LogManager.getLogger(GraphLogicStd.class);
	
	protected static final Pattern TAG_SPLITTER = Pattern.compile(",");
	protected static final Container.Factory CONTAINER_FACTORY = Lookup.getDefault().lookup(Container.Factory.class);
	protected final GraphDataSource graphDataSource;
	
	public GraphLogicStd(GraphDataSource graphDataSource) {
		this.graphDataSource = graphDataSource;
	}
	
	@Override
	public Container create(Integer graphId, Map<String, Object> extraParam) {
		LOGGER.debug("Gephi: Setup container, nodes and edges.");
		
		Report report = new Report();
		
		Container container = CONTAINER_FACTORY.newContainer();
		container.setReport(report);
		
		try (Connection con = graphDataSource.getConnection()) {
			Map<String, Object> graphParam = graphDataSource.getGraphByID(con, graphId);
			graphParam.putAll(extraParam);
			
			ContainerLoader cl = container.getLoader();
			
			setupDirectedMode(cl, graphParam);
			
			cl.addNodeColumn(KEY_URL, String.class);
			cl.addNodeColumn(KEY_TAG, String[].class);
			
			addNodes(graphId, con, cl, graphParam);
			addEdges(graphId, con, cl, graphParam);
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		finally {
			report.close();
		}
		
		return container;
	}
	
	private void setupDirectedMode(ContainerLoader cl, Map<String, Object> graphParam) {
		Integer directed = (Integer) graphParam.get("directed");
		if (Integer.valueOf(2).equals(directed)) {
			cl.setEdgeDefault(EdgeDirectionDefault.MIXED);
		}
		else if (Integer.valueOf(1).equals(directed)) {
			cl.setEdgeDefault(EdgeDirectionDefault.DIRECTED);
		}
		else {
			cl.setEdgeDefault(EdgeDirectionDefault.UNDIRECTED);
		}
	}
	
	protected void addNodes(Integer graphId, Connection con, ContainerLoader cl, Map<String, Object> graphParam) throws SQLException {
		String urlbaseTmp = (String) graphParam.get("url_base");
		
		final ElementDraft.Factory elementDraftFactory = cl.factory();
		final String urlbase = urlbaseTmp == null ? "/" : urlbaseTmp;
		
		graphDataSource.populateNodesForGraph(con, graphId, (num, name, tag) -> {
			String[] tags = tag != null ? TAG_SPLITTER.split(tag) : new String[]{};
			
			NodeDraft nd = elementDraftFactory.newNodeDraft(num.toString());
			nd.setLabel(name);
			nd.setValue(KEY_URL, urlbase + num);
			nd.setValue(KEY_TAG, tags);
			
			cl.addNode(nd);
			
			return true;
		});
	}
	
	protected void addEdges(Integer graphId, Connection con, ContainerLoader cl, Map<String, Object> graphParam) throws SQLException {
		final ElementDraft.Factory elementDraftFactory = cl.factory();
		
		graphDataSource.populateEdgesForGraph(con, graphId, (num, source, target, val) -> {
			EdgeDraft ed = elementDraftFactory.newEdgeDraft(num.toString());
			ed.setSource(cl.getNode(source.toString()));
			ed.setTarget(cl.getNode(target.toString()));
			ed.setWeight(val.floatValue());
			
			cl.addEdge(ed);
			
			return true;
		});
	}

}