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
import iabudiab.maven.plugins.dependencytrack.client.model.FindingsReport;
import iabudiab.maven.plugins.dependencytrack.client.model.ProjectMetrics;
import iabudiab.maven.plugins.dependencytrack.client.model.TokenResponse;

/**
 * Mojo for uploading a <a href="https://cyclonedx.org">CycloneDX</a> SBOM to
 * <a href="https://dependencytrack.org">Dependency-Track</a>
 * 
 * @author Iskandar Abudiab
 *
 */
@Mojo(name = "upload-bom", defaultPhase = LifecyclePhase.VERIFY, requiresOnline = true)
public class UploadBomMojo extends AbstractDependencyTrackMojo {

	/**
	 * CycloneDX SBOM directory.
	 */
	@Parameter(defaultValue = "${project.build.directory}", property = "artifactDir", required = true)
	private File artifactDirectory;

	/**
	 * CycloneDX SBOM filename.
	 */
	@Parameter(defaultValue = "bom.xml", property = "artifactName", required = true)
	private String artifactName;

	/**
	 * Polling timeout for the uploaded BOM token.
	 * 
	 * Upon uploading a BOM to Dependency-Track a token is returned, which can be
	 * checked for processing status. Once the token is processed, the findings are
	 * available and can be fetched for further analysis.
	 */
	@Parameter(defaultValue = "60", property = "tokenPollingDuration", required = true)
	private Integer tokenPollingDuration;

	/**
	 * Configurable thresholds for the allowed number of <code>critical</code>,
	 * <code>high</code>, <code>medium</code> and <code>low</code> findings from
	 * Dependency-Track, which would fail the build if not met.
	 */
	@Parameter(property = "securityGate", required = false)
	private SecurityGate securityGate = SecurityGate.strict();

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

		Project project;
		try {
			project = client.getProject(projectName, projectVersion);
		} catch (IOException | InterruptedException e) {
			throw new MojoExecutionException("Error loading project: ", e);
		}

		try {
			Boolean isProcessingToken = client.pollTokenProcessing(tokenResponse.getToken(), ForkJoinPool.commonPool()) //
					.completeOnTimeout(false, tokenPollingDuration, TimeUnit.SECONDS) //
					.get();

			if (isProcessingToken) {
				getLog().info("BOM token is still being processed, bailing out.");
				return;
			}

			List<Finding> findings = client.getProjectFindinds(project.getUuid());
			FindingsReport findingsReport = new FindingsReport(findings);
			getLog().info(findingsReport.printSummary());
		} catch (IOException | InterruptedException | ExecutionException e) {
			throw new PluginException("Error processing project findings: ", e);
		}

		ProjectMetrics projectMetrics;
		try {
			projectMetrics = client.getProjectMetrics(project.getUuid());
		} catch (IOException | InterruptedException e) {
			throw new PluginException("Error processing fetching metrics: ", e);
		}

		getLog().info(projectMetrics.printMetrics());
		getLog().info(securityGate.printThresholds());
		securityGate.applyOn(projectMetrics);
	}
}
