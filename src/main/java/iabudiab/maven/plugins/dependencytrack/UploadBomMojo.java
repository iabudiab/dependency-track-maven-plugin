package iabudiab.maven.plugins.dependencytrack;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

import iabudiab.maven.plugins.dependencytrack.client.model.*;
import iabudiab.maven.plugins.dependencytrack.dtrack.*;
import iabudiab.maven.plugins.dependencytrack.suppressions.Suppressions;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

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

	@Parameter(defaultValue = "3", property = "projectMetricsRetryLimit", required = true)
	private Integer projectMetricsRetryLimit;

	@Parameter(defaultValue = "5", property = "projectMetricsRetryDelay", required = true)
	private Integer projectMetricsRetryDelay;

	/**
	 * Configurable thresholds for the allowed number of <code>critical</code>,
	 * <code>high</code>, <code>medium</code> and <code>low</code> findings from
	 * Dependency-Track, which would fail the build if not met.
	 */
	@Parameter(property = "securityGate", required = false)
	private FindingsThresholdSecurityGate securityGate = FindingsThresholdSecurityGate.strict();

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

	/**
	 * Whether suppressions file should be created from effective suppressions.
	 */
	@Parameter(property = "cleanupSuppressions", defaultValue = "true", required = false)
	protected boolean cleanupSuppressions;

	/**
	 * Suppressions file path containing effective suppressions.
	 */
	@Parameter(defaultValue = "${project.build.directory}/dependency-track/suppressions.json", property = "cleanupSuppressionsFile", required = false)
	private String cleanupSuppressionsFile;


	@Override
	protected void logGoalConfiguration() {
		getLog().info("Using artifact directory        : " + artifactDirectory);
		getLog().info("Using artifact                  : " + artifactName);
		getLog().info("Upload matching suppressions    : " + uploadMatchingSuppressions);
		getLog().info("Reset expired suppressions      : " + resetExpiredSuppressions);
		getLog().info("ProjectMetrics retry delay      : " + projectMetricsRetryDelay);
		getLog().info("ProjectMetrics retry limit      : " + projectMetricsRetryLimit);

	}

	@Override
	protected void doWork(DTrack dtrack) throws DTrackException, MojoExecutionException {
		Path path = Paths.get(artifactDirectory.getPath(), artifactName);

		TokenResponse tokenResponse = dtrack.uploadBom(path);

		try {
			Path tokenFilePath = Paths.get(tokenFile);
			writeToPath(tokenResponse, tokenFilePath);
			getLog().info("Token has been written to: " + tokenFilePath);
		} catch (IOException e) {
			throw new MojoExecutionException("Error writing token: ", e);
		}

		if (!pollToken) {
			getLog().info("Token polling is disabled. Nothing more to do.");
			return;
		}

		boolean stillProcessingToken = dtrack.pollToken(tokenResponse.getToken(), tokenPollingDuration);

		if (stillProcessingToken) {
			getLog().info("Timeout while waiting for BOM token, bailing out.");
			return;
		}

		List<Finding> findings = dtrack.loadFindings();
		FindingsReport findingsReport = new FindingsReport(findings);
		getLog().info(InfoPrinter.print(findingsReport));

		ProjectMetrics projectMetrics = dtrack.loadProjectMetrics(projectMetricsRetryDelay, projectMetricsRetryLimit);
		getLog().info(InfoPrinter.print(projectMetrics));

		Suppressions suppressions = dtrack.getSuppressions();
		getLog().info(securityGate.print());
		getLog().info(suppressions.print());

		if (uploadMatchingSuppressions) {
			getLog().info("Applying suppressions");
			dtrack.applySuppressions(resetExpiredSuppressions);
		}

		SecurityGateDecision decision = securityGate.checkAgainst(findings, suppressions);
		decision.execute(getLog());

		if (cleanupSuppressions) {
			getLog().info("Cleaning suppression file");
			decision.getReport().cleanupSuppressionsFile(getLog(), cleanupSuppressionsFile);
		}
	}

	private void writeToPath(TokenResponse token, Path path) throws IOException {
		Files.createDirectories(path.getParent());
		byte[] tokenBytes = token.toString().getBytes(StandardCharsets.UTF_8);
		Files.write(path, tokenBytes,
			StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
	}
}
