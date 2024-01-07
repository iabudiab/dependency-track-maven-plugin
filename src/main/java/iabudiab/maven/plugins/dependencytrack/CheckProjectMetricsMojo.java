package iabudiab.maven.plugins.dependencytrack;

import iabudiab.maven.plugins.dependencytrack.client.model.Finding;
import iabudiab.maven.plugins.dependencytrack.client.model.ProjectMetrics;
import iabudiab.maven.plugins.dependencytrack.dtrack.*;
import iabudiab.maven.plugins.dependencytrack.suppressions.Suppressions;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.List;

/**
 * Mojo for checking a project's current metrics against a security gate.
 * 
 * @author Iskandar Abudiab
 *
 */
@Mojo(name = "check-metrics", defaultPhase = LifecyclePhase.VERIFY, requiresOnline = true)
public class CheckProjectMetricsMojo extends AbstractDependencyTrackMojo {

	/**
	 * Configurable thresholds for the allowed number of <code>critical</code>,
	 * <code>high</code>, <code>medium</code> and <code>low</code> findings from
	 * Dependency-Track, which would fail the build if not met.
	 */
	@Parameter(property = "securityGate", required = false)
	private FindingsThresholdSecurityGate securityGate = FindingsThresholdSecurityGate.strict();

	@Override
	protected void doWork(DTrack dtrack) throws DTrackException, MojoExecutionException {
		List<Finding> findings = dtrack.loadFindings();
		FindingsReport findingsReport = new FindingsReport(findings);
		getLog().info(InfoPrinter.print(findingsReport));

		ProjectMetrics projectMetrics = dtrack.loadProjectMetrics();
		getLog().info(InfoPrinter.print(projectMetrics));

		Suppressions suppressions = dtrack.getSuppressions();
		getLog().info(securityGate.print());
		getLog().info(suppressions.print());

		SecurityGateDecision decision = securityGate.checkAgainst(findings, suppressions);
		decision.execute(getLog());
	}
}
