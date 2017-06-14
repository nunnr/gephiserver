package org.nunn.gephiserver.server.graphing;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nunn.gephiserver.server.Props;
import org.nunn.gephiserver.server.graphing.impl.GraphExporterPDF;
import org.nunn.gephiserver.server.graphing.impl.GraphExporterSVG;
import org.nunn.gephiserver.server.graphing.impl.GraphLayoutStd;
import org.nunn.gephiserver.server.graphing.impl.GraphLogicRootNode;
import org.nunn.gephiserver.server.graphing.impl.GraphLogicStd;
import org.nunn.gephiserver.server.system.ExpiringCache;

/**
 * Controller for running Gephi renders. Executes each job serially, queuing pending requested jobs.
 * @author Rob
 */
public final class GraphBuilder {
	
	private static final Logger LOGGER = LogManager.getLogger(GraphBuilder.class);
	
	private final long jobTimeout;
	private final ThreadPoolExecutor executorService;
	private final ExpiringCache<String, Future<?>> resultCache;
	private final GraphDataSource graphDataSource;
	public final GraphLogic logicStd;
	public final GraphLogic logicRoot;
	public final GraphLayout layoutStd;
	public final GraphExporterSVG exporterSvg;
	public final GraphExporterPDF exporterPdf;
	
	public static final GraphBuilder INSTANCE = new GraphBuilder();
	
	private GraphBuilder() {
		jobTimeout = Props.INSTANCE.getPropertyAsLong("jobTimeout", 5000L);
		
		executorService = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
				new ArrayBlockingQueue<Runnable>(Props.INSTANCE.getPropertyAsInteger("jobQueueLength", 10))
		);
		executorService.prestartAllCoreThreads();
		
		resultCache = new ExpiringCache<>(
				jobTimeout + Props.INSTANCE.getPropertyAsLong("resultDiscardMillis", 30000L),
				(key, evictedEntry) -> {
					boolean cancelled = evictedEntry.cancel(true);
					executorService.purge();
					LOGGER.debug("Job {} expired before {}", key, (cancelled ? "completion" : "pick up"));
				}
		);
		
		graphDataSource = new GraphDataSource();
		LOGGER.debug(() -> graphDataSource.toString());
		
		logicStd = new GraphLogicStd(graphDataSource);
		logicRoot = new GraphLogicRootNode(graphDataSource);
		layoutStd = new GraphLayoutStd();
		exporterSvg = new GraphExporterSVG();
		exporterPdf = new GraphExporterPDF();
		
		LOGGER.debug("GraphBuilder instance created");
	}

	public void destroy() {
		executorService.shutdownNow();
		resultCache.disableExpiry();
		graphDataSource.close();
		graphDataSource.deregisterJdbcDrivers();
		LOGGER.info("Destroy tasks complete.");
	}
	
	private <OT> Future<GraphOutput<OT>> submit(GraphJob<OT> graphJob) {
		Future<GraphOutput<OT>> future;
		executorService.purge();
		try {
			future = executorService.submit(graphJob);
			LOGGER.debug("Job queue remaing capacity: {}", ()->{return executorService.getQueue().remainingCapacity();});
		}
		catch (RejectedExecutionException e) {
			LOGGER.warn("Rejected graph job {}", graphJob.uuid);
			throw e;
		}
		return future;
	}
	
	public <OT> GraphOutput<OT> doGraph(GraphLogic graphType, GraphLayout graphLayout, GraphExporter<OT> graphExporter, Integer graphId, Map<String, Object> extraParam)
			throws RejectedExecutionException, CancellationException, TimeoutException, InterruptedException, ExecutionException {
		
		GraphJob<OT> graphJob = new GraphJob<>(graphType, graphLayout, graphExporter, graphId, extraParam);
		Future<GraphOutput<OT>> future = submit(graphJob);
		GraphOutput<OT> result;
		try {
			result = future.get(jobTimeout, TimeUnit.MILLISECONDS);
		}
		catch (CancellationException | TimeoutException | InterruptedException | ExecutionException e) {
			future.cancel(true);
			executorService.purge();
			LOGGER.warn("{} on sync graph job {}", e.getClass().getSimpleName(), graphJob.uuid);
			throw e;
		}
		return result;
	}
	
	public <OT> String doGraphAsync(GraphLogic graphType, GraphLayout graphLayout, GraphExporter<OT> graphExporter, Integer graphId, Map<String, Object> extraParam)
			throws RejectedExecutionException {
		
		GraphJob<OT> graphJobAsync = new GraphJob<>(graphType, graphLayout, graphExporter, graphId, extraParam);
		Future<GraphOutput<OT>> future = submit(graphJobAsync);
		resultCache.put(graphJobAsync.uuid, future);
		return graphJobAsync.uuid;
	}
	
	private boolean resultIsDone(Future<?> result) {
		if (result == null) {
			throw new CancellationException("No result found.");
		}
		if (result.isCancelled()) {
			throw new CancellationException("Job was cancelled.");
		}
		return result.isDone();
	}
	
	public boolean isAsyncGraphRendered(String uuid) throws CancellationException {
		return resultIsDone(resultCache.get(uuid));
	}
	
	@SuppressWarnings("unchecked")
	public <OT> GraphOutput<OT> getAsyncResult(String uuid) throws CancellationException, InterruptedException, ExecutionException {
		GraphOutput<OT> graphOutput = null;
		Future<?> result = resultCache.get(uuid);
		if (resultIsDone(result)) {
			resultCache.remove(uuid);
			graphOutput = (GraphOutput<OT>) result.get();
		}
		return graphOutput;
	}

	public Map<Integer, String> listGraphs() {
		try (Connection con = graphDataSource.getConnection()) {
			return graphDataSource.listGraphs(con);
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void checkDatasource() {
		try (Connection con = graphDataSource.getConnection()) {
			graphDataSource.checkSchema(con);
			LOGGER.debug("Checked database schema: OK");
		}
		catch (SQLException e) {
			LOGGER.warn("Checked database schema: ERROR. Setup SQL script may need to be run.");
			throw new RuntimeException(e);
		}
	}
	
}