package org.nunn.gephiserver.server.graphing;

import java.util.Map;

public interface GraphLayout {
	
	/** Layout Gephi graph data set. Graph data ends up on Gephi default workspace.
	 * @return Map of meta data about the graph. 
	 * @throws InterruptedException Thrown during long running iterative processing */
	Map<String, Object> processGraph() throws InterruptedException;
	
}
