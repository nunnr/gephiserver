package org.nunn.gephiserver.graphing;

import java.util.Map;
import java.util.UUID;

public class GraphJobAsync<OT> extends GraphJob<OT> {

	public final String uuid;
	
	public GraphJobAsync(GraphLogic logicImpl, GraphLayout layoutImpl, GraphExporter<OT> graphExporter, Integer graphId, Map<String, Object> extraParam) {
		super(logicImpl, layoutImpl, graphExporter, graphId, extraParam);
		this.uuid = UUID.randomUUID().toString();
	}

}
