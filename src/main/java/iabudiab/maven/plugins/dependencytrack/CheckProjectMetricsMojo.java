package iabudiab.maven.plugins.dependencytrack;

import iabudiab.maven.plugins.dependencytrack.client.DTrackClient;
import iabudiab.maven.plugins.dependencytrack.client.model.Finding;
import iabudiab.maven.plugins.dependencytrack.client.model.FindingsReport;
import iabudiab.maven.plugins.dependencytrack.client.model.Project;
import iabudiab.maven.plugins.dependencytrack.client.model.ProjectMetrics;
import iabudiab.maven.plugins.dependencytrack.suppressions.Suppressions;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;
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
	private SecurityGate securityGate = SecurityGate.strict();

	@Override
	protected void doWork(DTrackClient client, Suppressions suppressions) throws MojoExecutionException, SecurityGateRejectionException {
		Project project;
		try {
			project = client.getProject(projectName, projectVersion);
		} catch (IOException e) {
			throw new MojoExecutionException("Error loading project: ", e);
		}

		List<Finding> findings;
		try {
			findings = client.getProjectFindings(project.getUuid());
			FindingsReport findingsReport = new FindingsReport(findings);
			getLog().info(findingsReport.printSummary());
		} catch (IOException e) {
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
		getLog().info(suppressions.printSummary());

		SecurityGate.SecurityReport securityReport = securityGate.applyOn(findings, suppressions);
		securityReport.execute(getLog());
	}
}
