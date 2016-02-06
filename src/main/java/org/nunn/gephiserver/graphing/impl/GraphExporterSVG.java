package org.nunn.gephiserver.graphing.impl;

import org.gephi.io.exporter.preview.SVGExporter;
import org.nunn.gephiserver.graphing.GraphExporter;
import org.nunn.gephiserver.system.StringBuilderWriter;

public class GraphExporterSVG implements GraphExporter<StringBuilderWriter> {

	/** Uses Gephi to lay out a network diagram.
	 * @return SVG network diagram. */
	public StringBuilderWriter export() {
		SVGExporter svgExporter = (SVGExporter) EXPORT_CONTROLLER.getExporter("svg");
		
		StringBuilderWriter sbw = new StringBuilderWriter(8192);
		EXPORT_CONTROLLER.exportWriter(sbw, svgExporter);
		
		return sbw;
	}
	
}