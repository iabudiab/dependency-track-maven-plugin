package iabudiab.maven.plugins.dependencytrack;

import iabudiab.maven.plugins.dependencytrack.client.DTrackClient;
import iabudiab.maven.plugins.dependencytrack.client.model.Finding;
import iabudiab.maven.plugins.dependencytrack.client.model.FindingsReport;
import iabudiab.maven.plugins.dependencytrack.client.model.Project;
import iabudiab.maven.plugins.dependencytrack.client.model.ProjectMetrics;
import org.apache.http.client.HttpResponseException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
	protected void doWork(DTrackClient client) throws MojoExecutionException, SecurityGateRejectionException {
		Project project;
		try {
			project = client.getProject(projectName, projectVersion);
		} catch (IOException e) {
			throw new MojoExecutionException("Error loading project: ", e);
		}

		try {
			List<Finding> findings = client.getProjectFindings(project.getUuid());
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
		securityGate.applyOn(projectMetrics);
	}
}
