package org.nunn.gephiserver.graphing;

import java.util.Map;

import org.gephi.io.importer.api.Container;

public interface GraphLogic {
	
	/** key for node URL anchor attribute */
	static final String KEY_URL = "nodeUrl";
	/** key for node-grouping attribute */
	static final String KEY_TAG = "nodeTag";
	
	Container create(Integer graphId, Map<String, Object> extraParam);
	
}
