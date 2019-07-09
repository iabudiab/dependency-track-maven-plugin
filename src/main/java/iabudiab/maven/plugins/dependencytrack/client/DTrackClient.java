package iabudiab.maven.plugins.dependencytrack.client;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.apache.maven.plugin.logging.Log;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import iabudiab.maven.plugins.dependencytrack.PluginException;
import iabudiab.maven.plugins.dependencytrack.client.model.BomSubmitRequest;
import iabudiab.maven.plugins.dependencytrack.client.model.Finding;
import iabudiab.maven.plugins.dependencytrack.client.model.ProjectMetrics;
import iabudiab.maven.plugins.dependencytrack.client.model.ScanSubmitRequest;
import iabudiab.maven.plugins.dependencytrack.client.model.TokenProcessedResponse;
import iabudiab.maven.plugins.dependencytrack.client.model.TokenResponse;

public class DTrackClient {

	private final String dependencyTrackApiKey;
	private final Log log;
	private final HttpClient client;
	private final URI baseUri;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public DTrackClient(String dependencyTrackUrl, String dependencyTrackApiKey, Log log) {
		this.dependencyTrackApiKey = dependencyTrackApiKey;
		this.log = log;
		this.client = HttpClient.newBuilder() //
				.connectTimeout(Duration.ofSeconds(30)) //
				.followRedirects(Redirect.NORMAL) //
				.build();
		try {
			this.baseUri = new URI(dependencyTrackUrl).resolve("/api/v1/");
			log.info("Using API v1 at: " + baseUri);
		} catch (URISyntaxException e) {
			throw new PluginException("Invalid DependencyTrack URL", e);
		}
	}

	private Builder newRequest() {
		return HttpRequest.newBuilder() //
				.header("X-Api-Key", dependencyTrackApiKey) //
				.header("Content-Type", "application/json") //
				.timeout(Duration.ofSeconds(60));
	}

	public void uploadScan(ScanSubmitRequest payload) throws IOException, InterruptedException {
		URI uri = baseUri.resolve("scan");
		String payloadAsString = objectMapper.writeValueAsString(payload);

		HttpRequest request = newRequest() //
				.PUT(BodyPublishers.ofString(payloadAsString, StandardCharsets.UTF_8)) //
				.uri(uri) //
				.build();

		log.info("Uploading scan artifact to: " + uri);
		HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
		checkResponseStatus(response);
	}

	public TokenResponse uploadBom(BomSubmitRequest payload) throws IOException, InterruptedException {
		URI uri = baseUri.resolve("bom");
		String payloadAsString = objectMapper.writeValueAsString(payload);

		HttpRequest request = newRequest() //
				.PUT(BodyPublishers.ofString(payloadAsString, StandardCharsets.UTF_8)) //
				.uri(uri) //
				.build();

		log.info("Uploading bom artifact to: " + uri);
		HttpResponse<String> httpResponse = client.send(request, BodyHandlers.ofString());
		checkResponseStatus(httpResponse);

		TokenResponse response = objectMapper.readValue(httpResponse.body(), TokenResponse.class);
		log.info("BOM response token: " + response.getToken());
		return response;
	}

	public TokenProcessedResponse checkIfTokenIsBeingProcessed(UUID token) throws IOException, InterruptedException {
		URI uri = baseUri.resolve("bom/token/" + token.toString());
		HttpRequest request = newRequest() //
				.GET().uri(uri) //
				.build();

		HttpResponse<String> httpResponse = client.send(request, BodyHandlers.ofString());
		checkResponseStatus(httpResponse);
		TokenProcessedResponse response = objectMapper.readValue(httpResponse.body(), TokenProcessedResponse.class);
		return response;
	}

	public CompletableFuture<Boolean> pollTokenProcessing(UUID token, Executor executor)
			throws IOException, InterruptedException {
		Supplier<Boolean> checkToken = () -> {
			try {
				log.info("Polling token [" +  Instant.now() + "]: " + token);
				return checkIfTokenIsBeingProcessed(token).isProcessing();
			} catch (IOException | InterruptedException e) {
				throw new CompletionException("Error during token polling", e);
			}
		};

		CompletableFuture<Boolean> result = CompletableFuture.supplyAsync(checkToken, executor) //
				.thenCompose(isProcessing -> {
					if (isProcessing) {
						try {
							log.info("Token is still being processed, will retery in 5 seconds");
							return pollTokenProcessing(token, CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS));
						} catch (IOException | InterruptedException e) {
							throw new CompletionException("Error during token polling", e);
						}
					}
					return CompletableFuture.completedFuture(isProcessing);
				});

		return result;
	}

	public List<Finding> getProjectFindinds(UUID projectId) throws IOException, InterruptedException {
		URI uri = baseUri.resolve("finding/project/" + projectId.toString());
		HttpRequest request = newRequest() //
				.GET().uri(uri) //
				.build();

		HttpResponse<String> httpResponse = client.send(request, BodyHandlers.ofString());
		checkResponseStatus(httpResponse);
		List<Finding> response = objectMapper.readValue(httpResponse.body(), new TypeReference<List<Finding>>() {});
		return response;
	}

	public ProjectMetrics getProjectMetrics(UUID projectId) throws IOException, InterruptedException {
		URI uri = baseUri.resolve("metrics/project/" + projectId.toString() + "/current");
		HttpRequest request = newRequest() //
				.GET().uri(uri) //
				.build();

		HttpResponse<String> httpResponse = client.send(request, BodyHandlers.ofString());
		checkResponseStatus(httpResponse);
		ProjectMetrics response = objectMapper.readValue(httpResponse.body(), ProjectMetrics.class);
		return response;
	}

	private void checkResponseStatus(HttpResponse<String> response) {
		int statusCode = response.statusCode();
		switch (statusCode) {
		case 200:
			log.debug("Request successful");
			break;
		case 400:
			log.error("Bad request. Probabaly an error in the plugin itself.");
			break;
		case 401:
			log.error("Unauthenticated. Check your API Key");
			break;
		case 403:
			log.error("Unauthorized. Check the permissions of the provided API Key. "
					+ "Required are: SCAN_UPLOAD and either PROJECT_CREATION_UPLOAD or PORTFOLIO_MANAGEMENT");
			break;
		default:
			log.warn("Received status code: " + statusCode);
			log.warn("Received response message: " + response.body());
			break;
		}
	}
}
