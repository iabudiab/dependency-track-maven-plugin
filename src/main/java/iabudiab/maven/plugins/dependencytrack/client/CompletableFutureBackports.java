package iabudiab.maven.plugins.dependencytrack.client;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;

/**
 * Code backported from CompletableFuture JDK9
 */
@UtilityClass
public class CompletableFutureBackports {

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
}
