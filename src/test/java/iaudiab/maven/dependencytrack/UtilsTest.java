package iaudiab.maven.dependencytrack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

import iabudiab.maven.plugins.dependencytrack.client.CompletableFutureUtils;
import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class UtilsTest {


	@Test
	public void retry_tests() throws URISyntaxException {

		{
			Log log = Mockito.mock(Log.class);

			Supplier<Integer> nullSupplier = () -> {
				return null;
			};

			assertThrows(CompletionException.class, () -> CompletableFutureUtils
				.retry(nullSupplier, Objects::isNull, 2, 0, 2, log).join());

			verify(log, times(1))
				.info(eq("Retry condition met, so retrying after '2' seconds (current retry count: '0'; max. retries: '2')"));
			verify(log, times(1))
				.info(eq("Retry condition met, so retrying after '2' seconds (current retry count: '1'; max. retries: '2')"));
			verify(log, times(1))
				.info(eq("Retry condition met, so retrying after '2' seconds (current retry count: '2'; max. retries: '2')"));
			verify(log, times(1))
				.warn(eq("Hit retry limit of '2'!"));
		}

		{
			Log log = Mockito.mock(Log.class);
			Supplier<Integer> valueSupplier = () -> {
				return 43;
			};

			assertEquals(43, CompletableFutureUtils
				.retry(valueSupplier, Objects::isNull, 2, 0, 2, log).join());

			verify(log, times(0)).info(anyString());
			verify(log, times(0)).warn(anyString());
		}

		{
			Log log = Mockito.mock(Log.class);

			ConcurrentLinkedQueue<Integer> values = new ConcurrentLinkedQueue<>(Arrays.asList(0, 1, 2, 3, 4, 5));
			Supplier<Integer> valueSupplier = values::remove;

			assertEquals(3, CompletableFutureUtils
				.retry(valueSupplier, value -> value < 3, 1, 0, 5, log).join());

			verify(log, times(3))
				.info(anyString());
			verify(log, times(1))
				.info(eq("Retry condition met, so retrying after '1' seconds (current retry count: '0'; max. retries: '5')"));
			verify(log, times(1))
				.info(eq("Retry condition met, so retrying after '1' seconds (current retry count: '1'; max. retries: '5')"));
			verify(log, times(1))
				.info(eq("Retry condition met, so retrying after '1' seconds (current retry count: '2'; max. retries: '5')"));
			verify(log, times(0))
				.warn(anyString());
		}
	}
}
