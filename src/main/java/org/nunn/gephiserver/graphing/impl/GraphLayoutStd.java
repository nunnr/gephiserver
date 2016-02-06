package org.nunn.gephiserver.graphing.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.layout.plugin.force.StepDisplacement;
import org.gephi.layout.plugin.force.yifanHu.YifanHuLayout;
import org.gephi.layout.plugin.labelAdjust.LabelAdjust;
import org.gephi.statistics.plugin.GraphDistance;
import org.nunn.gephiserver.graphing.GraphLayout;
import org.openide.util.Lookup;

public class GraphLayoutStd implements GraphLayout {
	
	private static final Logger LOGGER = LogManager.getLogger(GraphLayoutStd.class);
	
	@Override
	public Map<String, Object> processGraph() {
		LOGGER.debug("Gephi: Processing graph");
		
		Map<String, Object> feedback = new HashMap<String, Object>();

		GraphModel graphModel = Lookup.getDefault().lookup(GraphController.class).getGraphModel();
		
		// Layout - the overall style of graph - Here we run YifanHuLayout for 100 passes
		// This particular layout gives a cloud effect
		YifanHuLayout yifanHuLayout = new YifanHuLayout(null, new StepDisplacement(1f));
		yifanHuLayout.setGraphModel(graphModel);
		yifanHuLayout.resetPropertiesValues();
		yifanHuLayout.setOptimalDistance(250f);
		
		for (int i = 0; i < 100 && yifanHuLayout.canAlgo(); i++) {
			yifanHuLayout.goAlgo();
		}
		
		GraphDistance graphDistance = new GraphDistance();
		graphDistance.setDirected(true);
		graphDistance.execute(graphModel);
		
		// space out nodes to prevent text labels overlapping
		LabelAdjust labelAdjust = new LabelAdjust(null);
		labelAdjust.setGraphModel(graphModel);
		labelAdjust.resetPropertiesValues();
		labelAdjust.setAdjustBySize(true);
		labelAdjust.setSpeed(1.0d);
		labelAdjust.initAlgo();
		
		for (int i = 0; i < 100 && labelAdjust.canAlgo(); i++) {
			labelAdjust.goAlgo();
		}
		
		return feedback;
	}
	
}
