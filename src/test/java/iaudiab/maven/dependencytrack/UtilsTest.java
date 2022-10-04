package iaudiab.maven.dependencytrack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import iabudiab.maven.plugins.dependencytrack.Utils;

public class UtilsTest {


	@Test
	public void retry_tests() throws URISyntaxException {

		{
			Log log = Mockito.mock(Log.class);

			Supplier<Integer> nullSupplier = () -> {
				return null;
			};
			assertThrows(CompletionException.class, () -> Utils.retry(nullSupplier, result -> result == null, 2, 0, 2, log).join());
			Mockito.verify(log, Mockito.times(1)).info(Mockito.eq("retry condition met, so retrying after '2' seconds (current retry count: '0'; max. retries: '2')"));
			Mockito.verify(log, Mockito.times(1)).info(Mockito.eq("retry condition met, so retrying after '2' seconds (current retry count: '1'; max. retries: '2')"));
			Mockito.verify(log, Mockito.times(1)).info(Mockito.eq("retry condition met, so retrying after '2' seconds (current retry count: '2'; max. retries: '2')"));
			Mockito.verify(log, Mockito.times(1)).warn(Mockito.eq("hit retry limit of '2'!"));
		}

		{
			Log log = Mockito.mock(Log.class);
			Supplier<Integer> valueSupplier = () -> {
				return 43;
			};

			assertEquals(43, Utils.retry(valueSupplier, result -> result == null, 2, 0, 2, log).join());
			Mockito.verify(log, Mockito.times(0)).info(Mockito.anyString());
			Mockito.verify(log, Mockito.times(0)).warn(Mockito.anyString());
		}

		{
			Log log = Mockito.mock(Log.class);

			ConcurrentLinkedQueue<Integer> values = new ConcurrentLinkedQueue<>(Arrays.asList(0, 1, 2, 3, 4, 5));
			Supplier<Integer> valueSupplier = () -> {
				return values.remove();
			};

			assertEquals(3, Utils.retry(valueSupplier, value -> value < 3, 1, 0, 5, log).join());
			Mockito.verify(log, Mockito.times(3)).info(Mockito.anyString());
			Mockito.verify(log, Mockito.times(1)).info(Mockito.eq("retry condition met, so retrying after '1' seconds (current retry count: '0'; max. retries: '5')"));
			Mockito.verify(log, Mockito.times(1)).info(Mockito.eq("retry condition met, so retrying after '1' seconds (current retry count: '1'; max. retries: '5')"));
			Mockito.verify(log, Mockito.times(1)).info(Mockito.eq("retry condition met, so retrying after '1' seconds (current retry count: '2'; max. retries: '5')"));
			Mockito.verify(log, Mockito.times(0)).warn(Mockito.anyString());
		}
	}
}
