package org.nunn.gephiserver.graphing.impl;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gephi.appearance.api.AppearanceController;
import org.gephi.appearance.api.AppearanceModel;
import org.gephi.appearance.api.Partition;
import org.gephi.appearance.api.PartitionFunction;
import org.gephi.appearance.plugin.PartitionElementColorTransformer;
import org.gephi.appearance.plugin.palette.Palette;
import org.gephi.graph.api.Column;
import org.gephi.graph.api.DirectedGraph;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.layout.plugin.force.StepDisplacement;
import org.gephi.layout.plugin.force.yifanHu.YifanHuLayout;
import org.gephi.layout.plugin.labelAdjust.LabelAdjust;
import org.gephi.statistics.plugin.GraphDistance;
import org.gephi.statistics.plugin.Modularity;
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
		
		DirectedGraph graph = graphModel.getDirectedGraph();
		
		// Modularity algorithm - community detection
        Modularity modularity = new Modularity();
        modularity.execute(graphModel);
        // Partition by the column just created by Modularity algorithm
        Column modColumn = graphModel.getNodeTable().getColumn(Modularity.MODULARITY_CLASS);

        AppearanceController appearanceController = Lookup.getDefault().lookup(AppearanceController.class);
        AppearanceModel appearanceModel = appearanceController.getModel();
        PartitionFunction funcPartitionElementColorTransformer = (PartitionFunction) appearanceModel.getNodeFunction(
        		graph, modColumn, PartitionElementColorTransformer.class
        );

        Partition partition = funcPartitionElementColorTransformer.getPartition();
        Palette palette = randomPalette(partition.size());
        partition.setColors(palette.getColors());

        appearanceController.transform(funcPartitionElementColorTransformer);
		
		return feedback;
	}
	
	/** Following method replaces usage of gephi's PaletteManager.getInstance().randomPalette(colorCount),
	 * as using it trashes the JVM class loading...
	 * 
	 * Unfortunately initialising org.gephi.appearance.plugin.palette.PaletteManager will hit Netbeans'
	 * module preferences code, which in turn eventually overrides JVM URL handling. That is allowed via
	 * URL.setURLStreamHandlerFactory(fac), with caveat "This method can be called at most once in a given
	 * Java Virtual Machine" [java.net.URL]; full Error thrown if already set. Now as we are running in a
	 * servlet engine, URL handling will have already been customised. Netbeans' code then uses reflection
	 * to force things ...and ClassCircularityError is thrown randomly thereafter.
	 * 
	 * Mthod to generate a Gephi Pallette of random AWT colors.
	 * @param colorCount The number of colors required in the pallette.
	 * @return The built pallette.
	 */
	private Palette randomPalette(int colorCount) { 
		float colorCountF = (float) colorCount;
		Color[] colors = new Color[colorCount];
		for (int i = 0; i < colorCount; i++) {
			colors[i] = Color.getHSBColor(i / colorCountF, 0.8f, 0.8f);
		}
		return new Palette(colors);
	}
	
}
