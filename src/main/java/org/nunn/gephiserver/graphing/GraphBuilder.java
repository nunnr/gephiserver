package org.nunn.gephiserver.graphing;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nunn.gephiserver.Props;
import org.nunn.gephiserver.graphing.impl.GraphExporterPDF;
import org.nunn.gephiserver.graphing.impl.GraphExporterSVG;
import org.nunn.gephiserver.graphing.impl.GraphLayoutStd;
import org.nunn.gephiserver.graphing.impl.GraphLogicRootNode;
import org.nunn.gephiserver.graphing.impl.GraphLogicStd;
import org.nunn.gephiserver.system.ExpiringCache;

/**
 * Controller for running Gephi renders. Executes each job serially, queuing pending requested jobs.
 * @author Rob
 */
public final class GraphBuilder {
	
	private static final Logger LOGGER = LogManager.getLogger(GraphBuilder.class);
	
	/** max msec wait time for clients - we wait() the user request thread whilst rendering */
	private static final long MAX_REQUEST_JOB_WAIT = Props.INSTANCE.getPropertyAsLong("maxRequestJobWait", 5000L);
	
	private final BlockingQueue<Runnable> jobQueue;
	private final ExecutorService executorService;
	private final ExpiringCache<String, Future<?>> resultCache;
	
	private final GraphDataSource graphDataSource;
	
	public final GraphLogic logicStd;
	public final GraphLogic logicRoot;
	public final GraphLayout layoutStd;
	public final GraphExporterSVG exporterSvg;
	public final GraphExporterPDF exporterPdf;
	
	private static class FutureExpirationHandler implements ExpiringCache.ExpirationEventHandler<Future<?>> {
		@Override
		public void onExpiration(Future<?> evictedEntry) {
			evictedEntry.cancel(true);
		}
	}
	
	public static final GraphBuilder INSTANCE = new GraphBuilder();
	
	private GraphBuilder() {
		/** length of pending jobs queue */
		int jobQueueLength = Props.INSTANCE.getPropertyAsInteger("jobQueueLength", 10);
		jobQueue = new ArrayBlockingQueue<>(jobQueueLength);
		
		executorService = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, jobQueue);
		
		/** msec wait time for a client to take an async result before it is discarded */
		long resultDiscardMillis = Props.INSTANCE.getPropertyAsLong("resultDiscardMillis", 30000L);
		resultCache = new ExpiringCache<>(resultDiscardMillis, new FutureExpirationHandler());
		
		graphDataSource = new GraphDataSource(Props.INSTANCE.getPropertyAsString("graphSchema", "gephi"));
		
		logicStd = new GraphLogicStd(graphDataSource);
		logicRoot = new GraphLogicRootNode(graphDataSource);
		layoutStd = new GraphLayoutStd();
		exporterSvg = new GraphExporterSVG();
		exporterPdf = new GraphExporterPDF();
		
		LOGGER.info("GraphBuilder instance created");
	}
	
	public void initialize() {
		LOGGER.debug(() -> graphDataSource.toString());
		LOGGER.debug("Checking database schema.");
		graphDataSource.checkSchema();
		LOGGER.info("Initialize tasks complete.");
	}

	public void destroy() {
		executorService.shutdownNow();
		resultCache.disableExpiry();
		graphDataSource.close();
		graphDataSource.deregisterJdbcDrivers();
		LOGGER.info("Destroy tasks complete.");
	}
	
	public <OT> GraphOutput<OT> doGraph(GraphLogic graphType, GraphLayout graphLayout, GraphExporter<OT> graphExporter, Integer graphId, Map<String, Object> extraParam)
			throws RejectedExecutionException, CancellationException, TimeoutException, InterruptedException, ExecutionException {
		
		GraphJob<OT> graphJob = new GraphJob<>(graphType, graphLayout, graphExporter, graphId, extraParam);
		
		Future<GraphOutput<OT>> future = executorService.submit(graphJob);
		
		GraphOutput<OT> result;
		try {
			result = future.get(MAX_REQUEST_JOB_WAIT, TimeUnit.MILLISECONDS);
		}
		catch (CancellationException | TimeoutException | InterruptedException | ExecutionException e) {
			future.cancel(true);
			throw e;
		}
		return result;
	}
	
	public <OT> String doGraphAsync(GraphLogic graphType, GraphLayout graphLayout, GraphExporter<OT> graphExporter, Integer graphId, Map<String, Object> extraParam)
			throws RejectedExecutionException {
		
		GraphJobAsync<OT> graphJobAsync = new GraphJobAsync<>(graphType, graphLayout, graphExporter, graphId, extraParam);
		Future<GraphOutput<OT>> future = executorService.submit(graphJobAsync);
		resultCache.put(graphJobAsync.uuid, future);
		return graphJobAsync.uuid;
	}
	
	public boolean isAsyncGraphRendered(String uuid) throws CancellationException {
		Future<?> result = resultCache.get(uuid);
		if (result == null) {
			throw new CancellationException("No result matches given uuid.");
		}
		if (result.isCancelled()) {
			throw new CancellationException("Job was cancelled.");
		}
		return result.isDone();
	}
	
	@SuppressWarnings("unchecked")
	public <OT> GraphOutput<OT> getAsyncResult(String uuid) throws CancellationException, InterruptedException, ExecutionException {
		GraphOutput<OT> graphOutput = null;
		if (isAsyncGraphRendered(uuid)) {
			Future<?> result = resultCache.remove(uuid);
			if (result == null) {
				throw new CancellationException("No result matches given uuid.");
			}
			graphOutput = (GraphOutput<OT>) result.get();
		}
		return graphOutput;
	}
	
}
