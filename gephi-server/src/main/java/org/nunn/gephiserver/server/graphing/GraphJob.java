package org.nunn.gephiserver.server.graphing;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gephi.io.importer.api.Container;
import org.gephi.io.importer.api.ImportController;
import org.gephi.io.processor.spi.Processor;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.openide.util.Lookup;

/** Entry in rendering work queue */
public class GraphJob<OT> implements Callable<GraphOutput<OT>> {
	
	private static final Logger LOGGER = LogManager.getLogger(GraphJob.class);
	
	protected static final ProjectController PROJECT_CONTROLLER = Lookup.getDefault().lookup(ProjectController.class);
	protected static final ImportController IMPORT_CONTROLLER = Lookup.getDefault().lookup(ImportController.class);
	protected static final Processor DEFAULT_PROCESSOR = Lookup.getDefault().lookup(Processor.class);
	
	private final GraphLogic logicImpl;
	private final GraphLayout layoutImpl;
	private final GraphExporter<OT> graphExporter;
	private final Integer graphId;
	private final Map<String, Object> extraParam;

	public final String uuid;
	
	public GraphJob(GraphLogic logicImpl, GraphLayout layoutImpl, GraphExporter<OT> graphExporter, Integer graphId, Map<String, Object> extraParam) {
		this.logicImpl = logicImpl;
		this.layoutImpl = layoutImpl;
		this.graphExporter = graphExporter;
		this.graphId = graphId;
		this.extraParam = extraParam;
		this.uuid = UUID.randomUUID().toString();
	}

	/** Populate our node and edge data into Gephi Container, import to GraphModel, then export to final format. */
	@Override
	public GraphOutput<OT> call() throws CancellationException {
		long startedTime = System.currentTimeMillis();
		
		GraphOutput<OT> result;
		
		try {
			LOGGER.debug("Starting Gephi job");
			
			PROJECT_CONTROLLER.newProject();
			Workspace ws = PROJECT_CONTROLLER.getCurrentWorkspace();
			
			Container container = logicImpl.create(graphId, extraParam);
			
			checkInterrupted("before import controller processing");
			
			IMPORT_CONTROLLER.process(container, DEFAULT_PROCESSOR, ws);

			checkInterrupted("before layout processing");
			
			layoutImpl.processGraph();

			checkInterrupted("before export");
			
			OT output = graphExporter.export();

			checkInterrupted("before return");
			
			result = new GraphOutput<>(output);
		}
		catch (InterruptedException e) {
			LOGGER.debug("Graph job interrupted: {}", e.getMessage());
			throw new CancellationException("Graph job interrupted during processing");
		}
		finally {
			try {
				PROJECT_CONTROLLER.closeCurrentWorkspace();
				PROJECT_CONTROLLER.closeCurrentProject();
			}
			catch (Exception e) {
				LOGGER.warn("Workspace cleanup failed", e);
			}
			
			LOGGER.info("Graph job {} ran for {} msec", uuid, System.currentTimeMillis() - startedTime);
		}
		
		return result;
	}
	
	private void checkInterrupted(String msg) throws InterruptedException {
		if (Thread.interrupted()) {
			throw new InterruptedException(msg);
		}
	}
	
}