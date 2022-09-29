package iaudiab.maven.dependencytrack.client;

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

import iabudiab.maven.plugins.dependencytrack.client.DTrackClient;

public class DTrackClientTest {


	@Test
	public void retry_tests() throws URISyntaxException {

		{
			Log log = Mockito.mock(Log.class);
			DTrackClient client = new DTrackClient("http://localhost", "abc", log);

			Supplier<Integer> nullSupplier = () -> {
				return null;
			};
			assertThrows(CompletionException.class, () -> client.retry(nullSupplier, result -> result == null, 2, 0, 2).join());
			Mockito.verify(log, Mockito.times(1)).info(Mockito.eq("retry condition met, so retrying after '2' seconds (current retry count: '0'; max. retries: '2')"));
			Mockito.verify(log, Mockito.times(1)).info(Mockito.eq("retry condition met, so retrying after '2' seconds (current retry count: '1'; max. retries: '2')"));
			Mockito.verify(log, Mockito.times(1)).info(Mockito.eq("retry condition met, so retrying after '2' seconds (current retry count: '2'; max. retries: '2')"));
			Mockito.verify(log, Mockito.times(1)).warn(Mockito.eq("hit retry limit of '2'!"));
		}

		{
			Log log = Mockito.mock(Log.class);
			DTrackClient client = new DTrackClient("http://localhost", "abc", log);
			Supplier<Integer> valueSupplier = () -> {
				return 43;
			};

			assertEquals(43, client.retry(valueSupplier, result -> result == null, 2, 0, 2).join());
			// "Using api v1 ... " from DTrackClient creation
			Mockito.verify(log, Mockito.times(1)).info(Mockito.anyString());
			Mockito.verify(log, Mockito.times(0)).warn(Mockito.anyString());
		}

		{
			Log log = Mockito.mock(Log.class);
			DTrackClient client = new DTrackClient("http://localhost", "abc", log);

			ConcurrentLinkedQueue<Integer> values = new ConcurrentLinkedQueue<>(Arrays.asList(0, 1, 2, 3, 4, 5));
			Supplier<Integer> valueSupplier = () -> {
				return values.remove();
			};

			assertEquals(3, client.retry(valueSupplier, value -> value < 3, 1, 0, 5).join());
			Mockito.verify(log, Mockito.times(4)).info(Mockito.anyString());
			Mockito.verify(log, Mockito.times(1)).info(Mockito.eq("retry condition met, so retrying after '1' seconds (current retry count: '0'; max. retries: '5')"));
			Mockito.verify(log, Mockito.times(1)).info(Mockito.eq("retry condition met, so retrying after '1' seconds (current retry count: '1'; max. retries: '5')"));
			Mockito.verify(log, Mockito.times(1)).info(Mockito.eq("retry condition met, so retrying after '1' seconds (current retry count: '2'; max. retries: '5')"));
			Mockito.verify(log, Mockito.times(0)).warn(Mockito.anyString());
		}
	}
}
