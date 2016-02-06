package org.nunn.gephiserver.graphing;

import java.util.Map;

public interface GraphLayout {
	
	/** Layout Gephi graph data set. Graph data ends up on Gephi default workspace.
	 * @return Map of meta data about the graph. */
	Map<String, Object> processGraph();
	
}
