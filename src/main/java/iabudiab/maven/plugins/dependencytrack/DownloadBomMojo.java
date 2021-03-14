package iabudiab.maven.plugins.dependencytrack;

import iabudiab.maven.plugins.dependencytrack.client.DTrackClient;
import iabudiab.maven.plugins.dependencytrack.client.model.Project;
import iabudiab.maven.plugins.dependencytrack.client.model.ScanSubmitRequest;
import org.apache.http.client.HttpResponseException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

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
	 * Fails the goal if the project is not found.
	 */
	@Parameter(defaultValue = "false", property = "failedOnNotFound", required = false)
	private boolean failedOnNotFound;

	@Override
	protected void doWork(DTrackClient client) throws MojoExecutionException {
		Path path = Paths.get(destinationPath);
		try {
			Project project = client.getProject(projectName, projectVersion);
			client.downloadBom(project.getUuid(), path);
		} catch (HttpResponseException e) {
			handleProjectNotFound(e);
		} catch (IOException e) {
			throw new MojoExecutionException("Error downloading bom: ", e);
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
