package iabudiab.maven.plugins.dependencytrack;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import iabudiab.maven.plugins.dependencytrack.client.DTrackClient;
import iabudiab.maven.plugins.dependencytrack.client.model.ScanSubmitRequest;

@Mojo(name = "upload-scan", defaultPhase = LifecyclePhase.VERIFY)
public class UploadScanMojo extends AbstractDependencyTrackMojo {

	@Parameter(defaultValue = "${project.build.directory}", property = "artifactDir", required = true)
	private File artifactDirectory;

	@Parameter(defaultValue = "dependency-check-report.xml", property = "artifactName", required = true)
	private String artifactName;

	@Override
	protected void doWork(DTrackClient client) throws PluginException {
		Path path = Paths.get(artifactDirectory.getPath(), artifactName);
		String encodeArtifact = Utils.loadAndEncodeArtifactFile(path);

		ScanSubmitRequest payload = ScanSubmitRequest.builder() //
				.projectName(projectName) //
				.projectVersion(projectVersion) //
				.scan(encodeArtifact) //
				.autoCreate(true) //
				.build();

		try {
			client.uploadScan(payload);
		} catch (IOException | InterruptedException e) {
			throw new PluginException("Error uploading scan: ", e);
		}
	}
}
