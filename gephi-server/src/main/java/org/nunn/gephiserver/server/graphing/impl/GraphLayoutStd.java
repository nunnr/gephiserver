package org.nunn.gephiserver.server.graphing.impl;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gephi.appearance.api.AppearanceController;
import org.gephi.appearance.api.AppearanceModel;
import org.gephi.appearance.api.Function;
import org.gephi.appearance.api.Partition;
import org.gephi.appearance.api.PartitionFunction;
import org.gephi.appearance.plugin.PartitionElementColorTransformer;
import org.gephi.appearance.plugin.RankingNodeSizeTransformer;
import org.gephi.appearance.plugin.palette.Palette;
import org.gephi.graph.api.Column;
import org.gephi.graph.api.DirectedGraph;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.layout.plugin.force.StepDisplacement;
import org.gephi.layout.plugin.force.yifanHu.YifanHuLayout;
import org.gephi.layout.plugin.labelAdjust.LabelAdjust;
import org.gephi.preview.api.PreviewController;
import org.gephi.preview.api.PreviewModel;
import org.gephi.preview.api.PreviewProperty;
import org.gephi.preview.types.DependantColor;
import org.gephi.statistics.plugin.GraphDistance;
import org.gephi.statistics.plugin.Modularity;
import org.nunn.gephiserver.server.graphing.GraphLayout;
import org.openide.util.Lookup;

public class GraphLayoutStd implements GraphLayout {

	private static final Logger LOGGER = LogManager.getLogger(GraphLayoutStd.class);
	
	private final GraphController graphController = Lookup.getDefault().lookup(GraphController.class);
	private final AppearanceController appearanceController = Lookup.getDefault().lookup(AppearanceController.class);
	private final PreviewController previewController = Lookup.getDefault().lookup(PreviewController.class);

	@Override
	public Map<String, Object> processGraph() throws InterruptedException {
		LOGGER.debug("Gephi: Processing graph");

		Map<String, Object> feedback = new HashMap<>();

		GraphModel graphModel = graphController.getGraphModel();
		AppearanceModel appearanceModel = appearanceController.getModel();
		DirectedGraph graph = graphModel.getDirectedGraph();

		// YifanHuLayout layout gives a cloud effect
		YifanHuLayout yifanHuLayout = new YifanHuLayout(null, new StepDisplacement(1f));
		yifanHuLayout.setGraphModel(graphModel);
		yifanHuLayout.resetPropertiesValues();
		yifanHuLayout.setOptimalDistance(250f);
		for (int i = 0; i < 100 && yifanHuLayout.canAlgo(); i++) {
			yifanHuLayout.goAlgo();
			checkInterrupted("during YifanHuLayout");
		}
		
		//Get Centrality
		GraphDistance distance = new GraphDistance();
		distance.setDirected(true);
		distance.execute(graphModel);
		Column centralityColumn = graphModel.getNodeTable().getColumn(GraphDistance.BETWEENNESS);
		checkInterrupted("after GraphDistance");

		//Rank size by centrality
		Function func = appearanceModel.getNodeFunction(graph, centralityColumn, RankingNodeSizeTransformer.class);
		RankingNodeSizeTransformer centralityTransformer = (RankingNodeSizeTransformer) func.getTransformer();
		centralityTransformer.setMinSize(4);
		centralityTransformer.setMaxSize(20);
		appearanceController.transform(func);
		checkInterrupted("after RankingNodeSizeTransformer");

		// Modularity algorithm - community detection
		Modularity modularity = new Modularity();
		modularity.execute(graphModel);
		Column modColumn = graphModel.getNodeTable().getColumn(Modularity.MODULARITY_CLASS);
		checkInterrupted("after Modularity");

		// Partition by the column just created by Modularity algorithm
		func = appearanceModel.getNodeFunction(graph, modColumn, PartitionElementColorTransformer.class);
		Partition partition = ((PartitionFunction) func).getPartition();
		Palette palette = randomPalette(partition.size());
		partition.setColors(palette.getColors());
		appearanceController.transform(func);
		checkInterrupted("after PartitionElementColorTransformer");

		// space out nodes to prevent text labels overlapping
		LabelAdjust labelAdjust = new LabelAdjust(null);
		labelAdjust.setGraphModel(graphModel);
		labelAdjust.resetPropertiesValues();
		labelAdjust.setAdjustBySize(true);
		labelAdjust.setSpeed(8.0d);
		labelAdjust.initAlgo();
		for (int i = 0; i < 40 && labelAdjust.canAlgo(); i++) {
			labelAdjust.goAlgo();
			checkInterrupted("during LabelAdjust");
		}

		//Set 'show labels' option in Preview - and disable node size influence on text size
		PreviewModel previewModel = previewController.getModel();
		previewModel.getProperties().putValue(PreviewProperty.SHOW_NODE_LABELS, Boolean.TRUE);
		previewModel.getProperties().putValue(PreviewProperty.NODE_LABEL_PROPORTIONAL_SIZE, Boolean.FALSE);
		previewModel.getProperties().putValue(PreviewProperty.NODE_LABEL_OUTLINE_COLOR, new DependantColor(Color.WHITE));
		previewModel.getProperties().putValue(PreviewProperty.NODE_LABEL_OUTLINE_SIZE, 8);
		previewModel.getProperties().putValue(PreviewProperty.EDGE_OPACITY, 50);
		previewModel.getProperties().putValue(PreviewProperty.NODE_BORDER_WIDTH, 0);

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
	 * Method to generate a Gephi Pallette of random AWT colors.
	 * @param colorCount The number of colors required in the pallette.
	 * @return The built pallette.
	 */
	private Palette randomPalette(int colorCount) {
		float colorCountF = colorCount;
		Color[] colors = new Color[colorCount];
		for (int i = 0; i < colorCount; i++) {
			colors[i] = Color.getHSBColor(i / colorCountF, 0.8f, 0.8f);
		}
		return new Palette(colors);
	}

	private void checkInterrupted(String msg) throws InterruptedException {
		if (Thread.interrupted()) {
			throw new InterruptedException(msg);
		}
	}

}
