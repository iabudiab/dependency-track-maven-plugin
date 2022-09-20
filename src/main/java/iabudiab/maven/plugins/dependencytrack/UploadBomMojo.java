package iabudiab.maven.plugins.dependencytrack;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import iabudiab.maven.plugins.dependencytrack.client.model.*;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import iabudiab.maven.plugins.dependencytrack.client.DTrackClient;
import iabudiab.maven.plugins.dependencytrack.suppressions.Suppression;
import iabudiab.maven.plugins.dependencytrack.suppressions.Suppressions;

/**
 * Mojo for uploading a <a href="https://cyclonedx.org">CycloneDX</a> SBOM to
 * <a href="https://dependencytrack.org">Dependency-Track</a>
 *
 * @author Iskandar Abudiab
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
	 * Whether to poll the pending token for processing.
	 * <p>
	 * Default is <code>true</code>, which would poll the token for <code>tokenPollingDuration</code>
	 * and apply the <code>SecurityGate</code> afterwards.
	 * <p>
	 * If set to <code>false</code> then this goal would upload the BOM, write the token
	 * to a file at <code>tokenFile</code> and then exit.
	 */
	@Parameter(defaultValue = "true", property = "pollToken", required = true)
	private boolean pollToken;

	/**
	 * The token file path into which the token UUID value should be written.
	 */
	@Parameter(defaultValue = "${project.build.directory}/dependency-track/pendingToken", property = "tokenFile", required = true)
	private String tokenFile;

	/**
	 * Polling timeout for the uploaded BOM token.
	 * <p>
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

	/**
	 * Whether matching local suppressions for actual findings should be applied remotely in dependency track.
	 */
	@Parameter(property = "uploadMatchingSuppressions", defaultValue = "false", required = false)
	protected boolean uploadMatchingSuppressions;

	/**
	 * Whether matching local suppressions for actual findings, that are expired, should be reset in dependency track.
	 */
	@Parameter(property = "resetExpiredSuppressions", defaultValue = "true", required = false)
	protected boolean resetExpiredSuppressions;

	@Override
	protected void logGoalConfiguration() {
		getLog().info("Using artifact directory        : " + artifactDirectory);
		getLog().info("Using artifact                  : " + artifactName);
		getLog().info("Upload matching suppressions    : " + uploadMatchingSuppressions);
		getLog().info("Reset expired suppressions      : " + resetExpiredSuppressions);
	}

	@Override
	protected void doWork(DTrackClient client, Suppressions suppressions) throws MojoExecutionException, SecurityGateRejectionException {
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
		} catch (IOException e) {
			throw new MojoExecutionException("Error uploading bom: ", e);
		}

		try {
			Path tokenFilePath = Paths.get(tokenFile);
			Files.createDirectories(tokenFilePath.getParent());
			byte[] tokenBytes = tokenResponse.getToken().toString().getBytes(StandardCharsets.UTF_8);

			Files.write(tokenFilePath, tokenBytes,
				StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
			getLog().info("Token has been written to: " + tokenFilePath.toString());
		} catch (IOException e) {
			throw new MojoExecutionException("Error writing token: ", e);
		}

		if (!pollToken) {
			getLog().info("Token polling is disabled. Nothing more to do.");
			return;
		}

		Project project;
		try {
			project = client.getProject(projectName, projectVersion);
		} catch (IOException e) {
			throw new MojoExecutionException("Error loading project: ", e);
		}

		try {
			boolean isProcessingToken = client
				.pollTokenProcessing(tokenResponse.getToken(), ForkJoinPool.commonPool()) //
				.get(tokenPollingDuration, TimeUnit.SECONDS);

			if (isProcessingToken) {
				getLog().info("Timeout while waiting for BOM token, bailing out.");
				return;
			}
		} catch (TimeoutException | InterruptedException | ExecutionException e) {
			Thread.currentThread().interrupt();
			throw new MojoExecutionException("Error processing project findings: ", e);
		}

		List<Finding> findings;
		try {
			findings = client.getProjectFindings(project.getUuid());
			FindingsReport findingsReport = new FindingsReport(findings);
			getLog().info(findingsReport.printSummary());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		ProjectMetrics projectMetrics;
		try {
			projectMetrics = client.getProjectMetrics(project.getUuid());
		} catch (IOException e) {
			throw new MojoExecutionException("Error fetching project metrics: ", e);
		}

		getLog().info(projectMetrics.printMetrics());
		getLog().info(securityGate.printThresholds());
		getLog().info(suppressions.printSummary());

		uploadSuppressions(client, suppressions, project, findings);

		SecurityGate.SecurityReport securityReport = securityGate.applyOn(findings, suppressions);
		securityReport.execute(getLog());
	}

	private void uploadSuppressions(DTrackClient client, Suppressions suppressions, Project project, List<Finding> findings) throws MojoExecutionException {
		if (findings == null || !uploadMatchingSuppressions) {
			getLog().info("Skip checking for matching suppressions to be uploaded");
			return;
		}

		for (Finding finding : findings) {
			Suppression suppression = suppressions.hasSuppression(finding);
			if (suppression == null) {
				continue;
			}

			Analysis analysis = new Analysis();
			analysis.setProjectUuid(project.getUuid());
			analysis.setComponentUuid(finding.getComponent().getUuid());
			analysis.setVulnerabilityUuid(finding.getVulnerability().getUuid());
			analysis.setSuppressed(true);
			analysis.setComment(suppression.getNotes());
			analysis.setState(suppression.getState());
			analysis.setJustification(suppression.getJustification());
			analysis.setResponse(suppression.getResponse());

			if (resetExpiredSuppressions && suppression.isExpired()) {
				analysis.setSuppressed(false);
				analysis.setState(State.NOT_SET);
				analysis.setJustification(AnalysisJustification.NOT_SET);
				analysis.setResponse(AnalysisResponse.NOT_SET);
			}

			try {
				client.uploadAnalysis(analysis);
			} catch (IOException e) {
				throw new MojoExecutionException("Error uploading suppression analysis: ", e);
			}
		}
	}
}
