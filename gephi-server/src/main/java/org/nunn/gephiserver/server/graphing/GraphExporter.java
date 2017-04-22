package org.nunn.gephiserver.server.graphing;

import org.gephi.io.exporter.api.ExportController;
import org.openide.util.Lookup;

public interface GraphExporter<OT> {
	
	static final ExportController EXPORT_CONTROLLER = Lookup.getDefault().lookup(ExportController.class);
	
	OT export();
	
}