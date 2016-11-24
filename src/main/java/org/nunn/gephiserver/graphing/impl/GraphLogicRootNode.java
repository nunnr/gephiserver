package org.nunn.gephiserver.graphing.impl;

import java.sql.Connection;
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
		final Integer rootNodeId = (Integer) graphParam.get("rootNodeId");
		final Float upweight = (Float) graphParam.get("up_weight");
		final Float downweight = (Float) graphParam.get("down_weight");
		final ElementDraft.Factory elementDraftFactory = cl.factory();
		
		graphDataSource.populateEdgesForGraph(con, graphId, (num, source, target, val) -> {
			if (rootNodeId.equals(source) || rootNodeId.equals(target)) {
				val *= upweight;
			}
			else {
				val *= downweight;
			}
			
			EdgeDraft ed = elementDraftFactory.newEdgeDraft(num.toString());
			ed.setSource(cl.getNode(source.toString()));
			ed.setTarget(cl.getNode(target.toString()));
			ed.setWeight(val.floatValue());
			
			cl.addEdge(ed);
			
			return true;
		});
	}

}
