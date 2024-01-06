package iabudiab.maven.plugins.dependencytrack;

import iabudiab.maven.plugins.dependencytrack.client.model.*;
import iabudiab.maven.plugins.dependencytrack.dtrack.*;
import iabudiab.maven.plugins.dependencytrack.suppressions.Suppressions;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

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
	private FindingsThresholdSecurityGate securityGate = FindingsThresholdSecurityGate.strict();

	@Override
	protected void logGoalConfiguration() {
		getLog().info("Using token file                : " + tokenFile);
		getLog().info("Token value                     : " + tokenValue);
		getLog().info("Token poll duration             : " + tokenPollingDuration);
	}

	@Override
	protected void doWork(DTrack dtrack) throws DTrackException, MojoExecutionException {
		try {
			UUID token = loadToken();
			boolean isProcessingToken = dtrack.pollToken(token, tokenPollingDuration);

			if (isProcessingToken) {
				getLog().info("Timeout while waiting for BOM token, bailing out.");
				return;
			}
		} catch (IOException e) {
			throw new MojoExecutionException("Error loading token: ", e);
		}

		List<Finding> findings  = dtrack.loadFindings();
		FindingsReport findingsReport = new FindingsReport(findings);
		getLog().info(findingsReport.print());

		ProjectMetrics projectMetrics = dtrack.loadProjectMetrics();
		getLog().info(projectMetrics.print());

		Suppressions suppressions = dtrack.getSuppressions();
		getLog().info(securityGate.print());
		getLog().info(suppressions.print());

		SecurityReport securityReport = securityGate.applyOn(findings, suppressions);
		securityReport.execute(getLog());
	}

	private UUID loadToken() throws IOException {
		UUID token = null;
		Path tokenPath = Paths.get(tokenFile);

		if (Files.exists(tokenPath)) {
			getLog().info("Loading token from: " + tokenPath);
			byte[] bytes = Files.readAllBytes(tokenPath);
			String tokenString = new String(bytes, StandardCharsets.UTF_8);
			token = UUID.fromString(tokenString);
		}

		if (tokenValue != null && !tokenValue.isEmpty()) {
			getLog().info("Using provided tokenValue: " + tokenValue);
			token = UUID.fromString(tokenValue);
		}

		return token;
	}
}
