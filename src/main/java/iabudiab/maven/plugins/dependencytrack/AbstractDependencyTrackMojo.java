package iabudiab.maven.plugins.dependencytrack;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

import iabudiab.maven.plugins.dependencytrack.client.DTrackClient;

/**
 * Base class for all <a href=
 * "https://github.com/DependencyTrack/dependency-track">Dependency-Track</a>
 * related Mojos
 * 
 * @author Iskandar Abudiab
 *
 */
public abstract class AbstractDependencyTrackMojo extends AbstractMojo {

	/**
	 * The URL of the Dependency-Track Server
	 */
	@Parameter(property = "dependencyTrackUrl", required = true)
	private String dependencyTrackUrl;

	/**
	 * An API key for Dependency-Track.
	 * 
	 * The API key should have sufficient permissions:
	 * 
	 * <ul>
	 * <li><b>BOM_UPLOAD</b>: Allows the uploading of CycloneDX and SPDX BOMs
	 * <li><b>SCAN_UPLOAD</b>: Allows the uploading of Dependency-Check XML reports
	 * <li><b>VULNERABILITY_ANALYSIS</b>: Allows access to the findings API for
	 * trending and results
	 * <li><b>PROJECT_CREATION_UPLOAD</b>: Allows the dynamic creation of projects
	 * </ul>
	 * 
	 */
	@Parameter(property = "dependencyTrackApiKey", required = true)
	private String dependencyTrackApiKey;

	/**
	 * The project name in Dependency-Track. This name should be unique in a
	 * Dependency-Track installation.
	 * 
	 * If the project doesn't exist, it will be created automatically. The API key
	 * should have the <b>PROJECT_CREATION_UPLOAD</b> permission.
	 */
	@Parameter(property = "projectName", defaultValue = "${project.groupId}.${project.artifactId}", required = true)
	protected String projectName;

	/**
	 * The project version in Dependency-Track
	 */
	@Parameter(property = "projectVersion", defaultValue = "${project.version}", required = true)
	protected String projectVersion;

	/**
	 * Whether errors should fail the build.
	 */
	@Parameter(property = "failOnError", defaultValue = "true", required = true)
	private boolean failOnError;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		logConfiguration();

		try {
			DTrackClient client = new DTrackClient(dependencyTrackUrl, dependencyTrackApiKey, getLog());
			doWork(client);
		} catch (Exception e) {
			if (failOnError) {
				throw new MojoExecutionException("Error during plugin execution", e);
			} else {
				getLog().warn("failOnError: false => logging exception");
				getLog().warn("Error during plugin execution", e);
			}
		}
	}

	protected abstract void doWork(DTrackClient client) throws PluginException;

	private void logConfiguration() {
		getLog().info("DependencyTrack Maven Plugin");
		getLog().info("DependencyTrack URL   : " + dependencyTrackUrl);
		getLog().info("Project name          : " + projectName);
		getLog().info("Project version       : " + projectVersion);
	}
}
