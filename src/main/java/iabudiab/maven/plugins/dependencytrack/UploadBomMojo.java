package iabudiab.maven.plugins.dependencytrack;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import iabudiab.maven.plugins.dependencytrack.api.DTrackClient;
import iabudiab.maven.plugins.dependencytrack.client.model.BomSubmitRequest;
import iabudiab.maven.plugins.dependencytrack.client.model.TokenResponse;

@Mojo(name = "upload-bom", defaultPhase = LifecyclePhase.VERIFY)
public class UploadBomMojo extends AbstractDependencyTrackMojo {

	@Parameter(defaultValue = "${project.build.directory}", property = "artifactDir", required = true)
	private File artifactDirectory;

	@Parameter(defaultValue = "bom.xml", property = "artifactName", required = true)
	private String artifactName;

	@Override
	protected void doWork(DTrackClient client) throws PluginException {
		Path path = Paths.get(artifactDirectory.getPath(), artifactName);
		String encodeArtifact = Utils.loadAndEncodeArtifactFile(path);

		BomSubmitRequest payload = BomSubmitRequest.builder() //
				.projectName(projectName) //
				.projectVersion(projectVersion) //
				.bom(encodeArtifact) //
				.autoCreate(true) //
				.build();

		try {
			TokenResponse tokenResponse = client.uploadBom(payload);
		} catch (IOException | InterruptedException e) {
			throw new PluginException("Error uploading scan: ", e);
		}
	}
}
