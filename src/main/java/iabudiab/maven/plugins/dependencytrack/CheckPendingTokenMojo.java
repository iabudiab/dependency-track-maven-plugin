package iabudiab.maven.plugins.dependencytrack;

import iabudiab.maven.plugins.dependencytrack.client.DTrackClient;
import iabudiab.maven.plugins.dependencytrack.client.model.*;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Mojo for checking a pending token for processing status and applying
 * the security gate afterwards.
 * 
 * @author Iskandar Abudiab
 *
 */
@Mojo(name = "check-token", defaultPhase = LifecyclePhase.VERIFY, requiresOnline = true)
public class CheckPendingTokenMojo extends AbstractDependencyTrackMojo {

	/**
	 * Token file path containing the UUID of the pending token.
	 */
	@Parameter(defaultValue = "${project.build.directory}/dependency-track/pendingToken", property = "tokenFile")
	private String tokenFile;

	/**
	 * The UUID value of the pending token.
	 *
	 * This property takes precedence over <code>tokenFile</code> if both are set.
	 */
	@Parameter(property = "tokenValue")
	private String tokenValue;

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
	protected void doWork(DTrackClient client) throws MojoExecutionException, SecurityGateRejectionException {
		Project project;
		try {
			project = client.getProject(projectName, projectVersion);
		} catch (IOException e) {
			throw new MojoExecutionException("Error loading project: ", e);
		}

		try {
			UUID token = loadToken();
			boolean isProcessingToken = client
					.pollTokenProcessing(token, ForkJoinPool.commonPool()) //
					.get(tokenPollingDuration, TimeUnit.SECONDS);
			if (isProcessingToken) {
				getLog().info("Timeout while waiting for BOM token, bailing out.");
				return;
			}

			List<Finding> findings = client.getProjectFindinds(project.getUuid());
			FindingsReport findingsReport = new FindingsReport(findings);
			getLog().info(findingsReport.printSummary());
		} catch (TimeoutException| IOException | InterruptedException | ExecutionException e) {
			Thread.currentThread().interrupt();
			throw new MojoExecutionException("Error processing project findings: ", e);
		}

		ProjectMetrics projectMetrics;
		try {
			projectMetrics = client.getProjectMetrics(project.getUuid());
		} catch (IOException e) {
			throw new MojoExecutionException("Error fetching project metrics: ", e);
		}

		getLog().info(projectMetrics.printMetrics());
		getLog().info(securityGate.printThresholds());
		securityGate.applyOn(projectMetrics);
	}

	private UUID loadToken() throws IOException {
		UUID token = null;
		Path tokenPath = Paths.get(tokenFile);

		if (Files.exists(tokenPath)) {
			getLog().info("Loading token from: " + tokenPath.toString());
			byte[] bytes = Files.readAllBytes(tokenPath);
			String tokenString = new String(bytes, StandardCharsets.UTF_8);
			token = UUID.fromString(tokenString);
		}

		if (tokenValue != null && tokenValue != "") {
			getLog().info("Using provided tokenValue: " + tokenValue);
			token = UUID.fromString(tokenValue);
		}

		return token;
	}
}
