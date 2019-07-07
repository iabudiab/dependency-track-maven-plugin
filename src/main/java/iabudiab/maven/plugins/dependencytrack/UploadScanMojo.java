package iabudiab.maven.plugins.dependencytrack;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Base64;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import iabudiab.maven.plugins.dependencytrack.client.model.ScanSubmitRequest;

@Mojo(name = "upload-scan", defaultPhase = LifecyclePhase.VERIFY)
public class UploadScanMojo extends AbstractDependencyTrackMojo {

	@Parameter(defaultValue = "${project.build.directory}", property = "artifactDir", required = true)
	private File artifactDirectory;

	@Parameter(defaultValue = "dependency-check-report.xml", property = "artifactName", required = true)
	private String artifactName;

	@Override
	protected void doWork(HttpClient client, Builder requestBuilder, URI baseUri) throws PluginException {
		String encodeArtifact = loadAndEncodeArtifactFile();

		ScanSubmitRequest payload = ScanSubmitRequest.builder() //
				.projectName(projectName) //
				.projectVersion(projectVersion) //
				.scan(encodeArtifact) //
				.autoCreate(true) //
				.build();

		ObjectMapper objectMapper = new ObjectMapper();
		String payloadString;
		try {
			payloadString = objectMapper.writeValueAsString(payload);
		} catch (JsonProcessingException e) {
			throw new PluginException("Error serializing payload to JSON", e);
		}

		URI uri = baseUri.resolve("scan");
		HttpRequest request = requestBuilder.PUT(BodyPublishers.ofString(payloadString, StandardCharsets.UTF_8)) //
				.uri(uri) //
				.header("Content-Type", "application/json") //
				.timeout(Duration.ofSeconds(60)) //
				.build();

		HttpResponse<String> response;
		try {
			getLog().info("Uploading artifact to: " + uri);
			response = client.send(request, BodyHandlers.ofString());
		} catch (IOException | InterruptedException e) {
			throw new PluginException("Error uploading scan: ", e);
		}

		int statusCode = response.statusCode();
		switch (statusCode) {
		case 200:
			getLog().info("Upload successful");
			break;
		case 400:
			getLog().error("Bad request. Probabaly an error in the plugin itself.");
			break;
		case 401:
			getLog().error("Unauthenticated. Check your API Key");
			break;
		case 403:
			getLog().error("Unauthorized. Check the permissions of the provided API Key. "
					+ "Required are: SCAN_UPLOAD and either PROJECT_CREATION_UPLOAD or PORTFOLIO_MANAGEMENT");
			break;
		default:
			getLog().warn("Received status code: " + statusCode);
			getLog().warn("Received response message: " + response.body());
			break;
		}
	}

	protected String loadAndEncodeArtifactFile() throws PluginException {
		Path path = Paths.get(artifactDirectory.getPath(), artifactName);
		getLog().info("Loading artifact: " + path);

		if (!path.toFile().exists()) {
			throw new PluginException("Could not find artifact: " + path);
		}

		try {
			return Base64.getEncoder().encodeToString(Files.readAllBytes(path));
		} catch (IOException e) {
			throw new PluginException("Error enoding artifact", e);
		}
	}

}
