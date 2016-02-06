package org.nunn.gephiserver.graphing.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.gephi.io.importer.api.ContainerLoader;
import org.gephi.io.importer.api.EdgeDraft;
import org.gephi.io.importer.api.ElementDraft;
import org.nunn.gephiserver.graphing.GraphDataSource;

public class GraphLogicRootNode extends GraphLogicStd {
	
	public GraphLogicRootNode(GraphDataSource graphDataSource) {
		super(graphDataSource);
	}

	@Override
	protected void addEdges(Integer graphId, Connection con, ContainerLoader cl, Map<String, Object> graphParam) throws SQLException {
		Integer rootNodeId = (Integer) graphParam.get("rootNodeId");
		Double upweight = (Double) graphParam.get("upweight");
		Double downweight = (Double) graphParam.get("downweight");
		
		ElementDraft.Factory elementDraftFactory = cl.factory();
		
		PreparedStatement ps = con.prepareStatement("select * from gephi.edge where graph = ?");
		ps.setInt(1, graphId);
		
		ResultSet rs = ps.executeQuery();
		
		while (rs.next()) {
			Integer num = rs.getInt("num");
			Integer source = rs.getInt("source");
			Integer target = rs.getInt("target");
			Double value = rs.getDouble("value");
			
			if (rootNodeId.equals(source) || rootNodeId.equals(target)) {
				value *= upweight;
			}
			else {
				value *= downweight;
			}
			
			EdgeDraft ed = elementDraftFactory.newEdgeDraft(num.toString());
			ed.setSource(cl.getNode(source.toString()));
			ed.setTarget(cl.getNode(target.toString()));
			ed.setWeight(value.floatValue());
			
			cl.addEdge(ed);
		}
		
		ps.close();
	}

}
