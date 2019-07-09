package iabudiab.maven.plugins.dependencytrack;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;

import iabudiab.maven.plugins.dependencytrack.client.DTrackClient;
import iabudiab.maven.plugins.dependencytrack.client.model.BomSubmitRequest;
import iabudiab.maven.plugins.dependencytrack.client.model.Finding;
import iabudiab.maven.plugins.dependencytrack.client.model.TokenResponse;

@Mojo(name = "upload-bom", defaultPhase = LifecyclePhase.VERIFY)
public class UploadBomMojo extends AbstractDependencyTrackMojo {

	@Parameter(defaultValue = "${project.build.directory}", property = "artifactDir", required = true)
	private File artifactDirectory;

	@Parameter(defaultValue = "bom.xml", property = "artifactName", required = true)
	private String artifactName;

	@Parameter(defaultValue = "60", property = "tokenPollingDuration", required = true)
	private Integer tokenPollingDuration;

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

		TokenResponse tokenResponse;
		try {
			tokenResponse = client.uploadBom(payload);
		} catch (IOException | InterruptedException e) {
			throw new PluginException("Error uploading bom: ", e);
		}

		if (StringUtils.isEmpty(projectId)) {
			getLog().info("Skipping Findings Analysis and Security Gate due to missing Project ID.");
			getLog().info("Set the ProjectID in order to use Findings Analysis and Security Gate features.");
			return;
		}

		try {
			Boolean isProcessingToken = client.pollTokenProcessing(tokenResponse.getToken(), ForkJoinPool.commonPool()) //
					.completeOnTimeout(false, tokenPollingDuration, TimeUnit.SECONDS) //
					.get();

			if (isProcessingToken) {
				getLog().info("BOM token is still being processed, bailing out.");
				return;
			}

			List<Finding> findinds = client.getProjectFindinds(UUID.fromString(projectId));
		} catch (IOException | InterruptedException | ExecutionException e) {
			throw new PluginException("Error processing project findigns: ", e);
		}

	}
}
