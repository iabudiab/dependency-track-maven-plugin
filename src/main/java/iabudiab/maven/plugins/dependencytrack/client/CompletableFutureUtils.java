package iabudiab.maven.plugins.dependencytrack.client;

import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Supplier;

import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;
import org.apache.maven.plugin.logging.Log;

/**
 * Code backported from CompletableFuture JDK9
 */
@UtilityClass
public class CompletableFutureUtils {

	private static final Executor POOL = ForkJoinPool.commonPool().getParallelism() > 1 //
			? ForkJoinPool.commonPool() //
			: runnable -> new Thread(runnable).start();

	public static Executor delayedExecutor(long delay, TimeUnit unit) {
		return new DelayedExecutor(delay, unit, POOL);
	}

	@RequiredArgsConstructor
	private static final class DelayedExecutor implements Executor {

		private static final ScheduledThreadPoolExecutor DELAYED = new ScheduledThreadPoolExecutor(1, runnable -> {
			Thread thread = new Thread(runnable);
			thread.setDaemon(true);
			return thread;
		});

		static {
			DELAYED.setRemoveOnCancelPolicy(true);
		}

		private final long delay;
		private final TimeUnit unit;
		private final Executor executor;

		public void execute(Runnable r) {
			DELAYED.schedule(() -> executor.execute(r), delay, unit);
		}
	}

	public <R> CompletableFuture<R> retry(Supplier<R> supplier, Function<R, Boolean> retryCondition, int retryDelay, int retryCount, int retryLimit, Log log) {
		if(retryCount > retryLimit) {
			log.warn("Hit retry limit of '"+ retryLimit +"'!");
			throw new CompletionException("Hit retry limit of '"+ retryLimit +"'", null);
		}

		Executor executor = (retryCount == 0)
			? ForkJoinPool.commonPool()
			: CompletableFutureUtils.delayedExecutor(retryDelay, TimeUnit.SECONDS);

		return CompletableFuture.supplyAsync(supplier, executor)
			.thenCompose(result -> {
				if(retryCondition.apply(result)) {
					log.info("Retry condition met, so retrying after '" + retryDelay +
						"' seconds (current retry count: '" + retryCount +
						"'; max. retries: '" + retryLimit + "')");

					return retry(supplier, retryCondition, retryDelay, retryCount+1, retryLimit, log);
				}
				return CompletableFuture.completedFuture(result);
			});
	}
}
