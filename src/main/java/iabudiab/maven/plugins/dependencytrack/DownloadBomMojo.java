package iabudiab.maven.plugins.dependencytrack;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import iabudiab.maven.plugins.dependencytrack.client.DTrackClient;
import iabudiab.maven.plugins.dependencytrack.client.model.Project;
import iabudiab.maven.plugins.dependencytrack.suppressions.Suppressions;
import org.apache.http.client.HttpResponseException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Mojo for downloading a project's
 * <a href="https://cyclonedx.org">CycloneDX</a>
 * SBoM from <a href="https://github.com/DependencyTrack/dependency-track">Dependency-Track</a>
 * 
 * @author Iskandar Abudiab
 *
 */
@Mojo(name = "download-bom", defaultPhase = LifecyclePhase.VERIFY, requiresOnline = true)
public class DownloadBomMojo extends AbstractDependencyTrackMojo {

	/**
	 * Destination file path for the downloaded SBoM.
	 */
	@Parameter(defaultValue = "${project.build.directory}/${project.build.finalName}_bom.xml", property = "destinationPath", required = false)
	private String destinationPath;

	/**
	 * The BOM format to download from Dependency Track.
	 */
	@Parameter(defaultValue = "XML", property = "outputFormat", required = false)
	private BomFormat outputFormat;

	/**
	 * Fails the goal if the project is not found.
	 */
	@Parameter(defaultValue = "false", property = "failedOnNotFound", required = false)
	private boolean failedOnNotFound;

	@Override
	protected void logGoalConfiguration() {
		getLog().info("Destination path                : " + destinationPath);
		getLog().info("Output format                   : " + outputFormat);
	}

	@Override
	protected void doWork(DTrackClient client, Suppressions suppressions) throws MojoExecutionException {
		validateArguments();

		Path path = Paths.get(destinationPath);
		try {
			Project project = client.getProject(projectName, projectVersion);
			client.downloadBom(project.getUuid(), path, outputFormat);
		} catch (HttpResponseException e) {
			handleProjectNotFound(e);
		} catch (IOException e) {
			throw new MojoExecutionException("Error downloading bom: ", e);
		}
	}

	private void validateArguments() throws MojoExecutionException {
		if (outputFormat == BomFormat.XML && !destinationPath.endsWith(".xml")) {
			throw new MojoExecutionException("XML format specified but destinationPath doesn't have corresponding xml extension");
		}

		if (outputFormat == BomFormat.JSON && !destinationPath.endsWith(".json")) {
			throw new MojoExecutionException("JSON format specified but destinationPath doesn't have corresponding json extension");
		}
	}

	private void handleProjectNotFound(HttpResponseException e) throws MojoExecutionException {
		if (e.getStatusCode() == 404) {
			if (failedOnNotFound) {
				throw new MojoExecutionException("Project not found: ", e);
			} else {
				getLog().info("failedOnNotFound=false, ignoring project not found: " + projectName + "-" + projectVersion);
			}
		} else {
			throw new MojoExecutionException("Error downloading bom: ", e);
		}
	}
}
