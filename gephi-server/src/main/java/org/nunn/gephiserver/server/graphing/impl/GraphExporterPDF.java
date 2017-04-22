package org.nunn.gephiserver.server.graphing.impl;

import java.io.ByteArrayOutputStream;

import org.gephi.io.exporter.preview.PDFExporter;
import org.nunn.gephiserver.server.graphing.GraphExporter;

public class GraphExporterPDF implements GraphExporter<ByteArrayOutputStream> {

	/** Uses Gephi to lay out a network diagram.
	 * @return PDF data network diagram. */
	@Override
	public ByteArrayOutputStream export() {
		PDFExporter pdfExporter = (PDFExporter) EXPORT_CONTROLLER.getExporter("pdf");
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		EXPORT_CONTROLLER.exportStream(baos, pdfExporter);
		
		return baos;
	}
	
}