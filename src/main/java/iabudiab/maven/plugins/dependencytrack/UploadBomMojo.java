package iabudiab.maven.plugins.dependencytrack;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import iabudiab.maven.plugins.dependencytrack.api.DTrackClient;
import iabudiab.maven.plugins.dependencytrack.client.model.BomSubmitRequest;
import iabudiab.maven.plugins.dependencytrack.client.model.TokenResponse;

@Mojo(name = "upload-bom", defaultPhase = LifecyclePhase.VERIFY)
public class UploadBomMojo extends AbstractDependencyTrackMojo {

	@Parameter(defaultValue = "${project.build.directory}", property = "artifactDir", required = true)
	private File artifactDirectory;

	@Parameter(defaultValue = "bomt.xml", property = "artifactName", required = true)
	private String artifactName;

	@Override
	protected void doWork(DTrackClient client) throws PluginException {
		String encodeArtifact = loadAndEncodeArtifactFile();

		BomSubmitRequest payload = BomSubmitRequest.builder() //
				.projectName(projectName) //
				.projectVersion(projectVersion) //
				.bom(encodeArtifact) //
				.autoCreate(true) //
				.build();

		ObjectMapper objectMapper = new ObjectMapper();
		String encodedArtifact;
		try {
			encodedArtifact = objectMapper.writeValueAsString(payload);
		} catch (JsonProcessingException e) {
			throw new PluginException("Error serializing payload to JSON", e);
		}

		try {
			TokenResponse tokenResponse = client.uploadBom(encodedArtifact);
		} catch (IOException | InterruptedException e) {
			throw new PluginException("Error uploading scan: ", e);
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