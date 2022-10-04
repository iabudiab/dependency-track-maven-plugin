package iabudiab.maven.plugins.dependencytrack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import iabudiab.maven.plugins.dependencytrack.client.CompletableFutureBackports;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Utils {

	public String loadAndEncodeArtifactFile(Path path) throws MojoExecutionException {
		if (!path.toFile().exists()) {
			throw new MojoExecutionException("Could not find artifact: " + path);
		}

		try {
			return Base64.getEncoder().encodeToString(Files.readAllBytes(path));
		} catch (IOException e) {
			throw new MojoExecutionException("Error enoding artifact", e);
		}
	}


	public <R> CompletableFuture<R> retry(Supplier<R> supplier, Function<R, Boolean> retryCondition, int retryDelay, int retryCount, int retryLimit, Log log) {
		if(retryCount > retryLimit) {
			log.warn("hit retry limit of '"+ retryLimit +"'!");
			throw new CompletionException("hit retry limit of '"+ retryLimit +"'", null);
		}

		Executor executor = (retryCount == 0) ? ForkJoinPool.commonPool() : CompletableFutureBackports.delayedExecutor(retryDelay, TimeUnit.SECONDS);
		return CompletableFuture.supplyAsync(supplier, executor)
				.thenCompose(result -> {
					if(retryCondition.apply(result)) {
						log.info("retry condition met, so retrying after '"+ retryDelay +"' seconds (current retry count: '"+ retryCount +"'; max. retries: '"+ retryLimit +"')");
						return retry(supplier, retryCondition, retryDelay, retryCount+1, retryLimit, log);
					}
					return CompletableFuture.completedFuture(result);
				});
	}
}
