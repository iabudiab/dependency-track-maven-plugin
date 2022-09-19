package iabudiab.maven.plugins.dependencytrack;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import iabudiab.maven.plugins.dependencytrack.suppressions.Suppressions;
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

	/**
	 * Path to a suppressions file containing suppression definitions to be applied before checking the security gate.
	 */
	@Parameter(property = "suppressions", defaultValue = "${project.basedir}/suppressions.json", required = false)
	private String suppressionsFile;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		logConfiguration();
		logGoalConfiguration();

		try {
			DTrackClient client = new DTrackClient(dependencyTrackUrl, dependencyTrackApiKey, getLog());
			Suppressions suppressions = loadSuppressions();
			doWork(client, suppressions);
		} catch (URISyntaxException e) {
			throw new MojoExecutionException("Error during plugin execution", e);
		} catch (MojoFailureException e) {
			handleFailureException(e);
		}
	}

	private Suppressions loadSuppressions() {
		Path suppressionsPath = Paths.get(suppressionsFile);

		if (!Files.exists(suppressionsPath)) {
			return Suppressions.none();
		}

		try {
			getLog().info("Loading suppressions from: " + suppressionsPath);
			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.registerModule(new JavaTimeModule());
			return objectMapper.readValue(suppressionsPath.toFile(), Suppressions.class);
		} catch (IOException e) {
			getLog().warn("Couldn't load suppressions: " + e.getMessage());
			return Suppressions.none();
		}
	}

	private void handleFailureException(MojoFailureException e) throws MojoFailureException {
		if (failOnError) {
			throw e;
		} else {
			if (getLog().isDebugEnabled()) {
				getLog().debug(e);
			} else {
				getLog().warn(e.getMessage());
			}
		}
	}

	protected void logGoalConfiguration() {
		// NOOP
	}

	protected abstract void doWork(DTrackClient client, Suppressions suppressions) throws MojoExecutionException, MojoFailureException;

	private void logConfiguration() {
		getLog().info("DependencyTrack Maven Plugin");
		getLog().info("DependencyTrack URL             : " + dependencyTrackUrl);
		getLog().info("Project name                    : " + projectName);
		getLog().info("Project version                 : " + projectVersion);
	}
}
