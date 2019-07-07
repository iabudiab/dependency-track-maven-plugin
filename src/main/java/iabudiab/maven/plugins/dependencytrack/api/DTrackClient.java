package iabudiab.maven.plugins.dependencytrack.api;

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

import org.apache.maven.plugin.logging.Log;

import com.fasterxml.jackson.databind.ObjectMapper;

import iabudiab.maven.plugins.dependencytrack.PluginException;
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

	public void uploadScan(String encodedArtifact) throws IOException, InterruptedException {
		URI uri = baseUri.resolve("scan");
		HttpRequest request = newRequest() //
				.PUT(BodyPublishers.ofString(encodedArtifact, StandardCharsets.UTF_8)) //
				.uri(uri) //
				.build();

		log.info("Uploading scan artifact to: " + uri);
		HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
		checkResponseStatus(response);
	}

	public TokenResponse uploadBom(String encodedArtifact) throws IOException, InterruptedException {
		URI uri = baseUri.resolve("bom");
		HttpRequest request = newRequest() //
				.PUT(BodyPublishers.ofString(encodedArtifact, StandardCharsets.UTF_8)) //
				.uri(uri) //
				.build();

		log.info("Uploading scan artifact to: " + uri);
		HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
		checkResponseStatus(response);

		TokenResponse tokenResponse = objectMapper.readValue(response.body(), TokenResponse.class);
		return tokenResponse;
	}

	private void checkResponseStatus(HttpResponse<String> response) {
		int statusCode = response.statusCode();
		switch (statusCode) {
		case 200:
			log.info("Request successful");
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
