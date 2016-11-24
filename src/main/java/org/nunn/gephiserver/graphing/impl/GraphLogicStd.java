package org.nunn.gephiserver.graphing.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
import org.nunn.gephiserver.graphing.GraphDataSource;
import org.nunn.gephiserver.graphing.GraphLogic;
import org.nunn.gephiserver.system.SqlTools;
import org.openide.util.Lookup;

public class GraphLogicStd implements GraphLogic {
	
	private static final Logger LOGGER = LogManager.getLogger(GraphLogicStd.class);

	private static final Container.Factory CONTAINER_FACTORY = Lookup.getDefault().lookup(Container.Factory.class);
	
	private final GraphDataSource graphDataSource;
	
	public GraphLogicStd(GraphDataSource graphDataSource) {
		this.graphDataSource = graphDataSource;
	}
	
	@Override
	public Container create(Integer graphId, Map<String, Object> extraParam) {
		LOGGER.debug("Gephi: Setup container, nodes and edges.");
		
		Container container = CONTAINER_FACTORY.newContainer();
		container.setReport(new Report());
		
		Connection con = null;
		try {
			con = graphDataSource.getConnection();
		
			Map<String, Object> graphParam = getGraphParameters(con, graphId);
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
			if (con != null) {
				try {
					con.close();
				}
				catch (SQLException e) {
					LOGGER.error("Error closing database connection.", e);
				}
			}
		}
		
		return container;
	}

	private Map<String, Object> getGraphParameters(Connection con, Integer id) throws SQLException {
		PreparedStatement ps = con.prepareStatement("select * from gephi.graph where id = ?");
		ps.setInt(1, id);
		
		ResultSet rs = ps.executeQuery();
		Map<String, Object> result = SqlTools.convertResultSetToSingle(rs);
		
		ps.close();
		
		return result;
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
		String urlbase = (String) graphParam.get("urlbase");
		if (urlbase == null) {
			urlbase = "/";
		}
		
		ElementDraft.Factory elementDraftFactory = cl.factory();
		
		Pattern tagSplitter = Pattern.compile(",");
		
		PreparedStatement ps = con.prepareStatement("select * from gephi.node where graph = ?");
		ps.setInt(1, graphId);
		
		ResultSet rs = ps.executeQuery();
		
		while (rs.next()) {
			Integer num = rs.getInt("num");
			String name = rs.getString("name");
			String tag = rs.getString("tag");
			
			String[] tags = tag != null ? tagSplitter.split(tag) : new String[]{};
			
			NodeDraft nd = elementDraftFactory.newNodeDraft(num.toString());
			nd.setLabel(name);
			nd.setValue(KEY_URL, urlbase + num);
			nd.setValue(KEY_TAG, tags);
			
			cl.addNode(nd);
		}
		
		ps.close();
	}
	
	protected void addEdges(Integer graphId, Connection con, ContainerLoader cl, Map<String, Object> graphParam) throws SQLException {
		ElementDraft.Factory elementDraftFactory = cl.factory();
		
		PreparedStatement ps = con.prepareStatement("select * from gephi.edge where graph = ?");
		ps.setInt(1, graphId);
		
		ResultSet rs = ps.executeQuery();
		
		while (rs.next()) {
			Integer num = rs.getInt("num");
			Integer source = rs.getInt("source");
			Integer target = rs.getInt("target");
			Double value = rs.getDouble("value");
			
			EdgeDraft ed = elementDraftFactory.newEdgeDraft(num.toString());
			ed.setSource(cl.getNode(source.toString()));
			ed.setTarget(cl.getNode(target.toString()));
			ed.setWeight(value.floatValue());
			
			cl.addEdge(ed);
		}
		
		ps.close();
	}

}
