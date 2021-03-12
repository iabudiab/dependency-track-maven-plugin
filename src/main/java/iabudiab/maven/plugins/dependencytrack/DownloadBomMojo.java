package iabudiab.maven.plugins.dependencytrack;

import iabudiab.maven.plugins.dependencytrack.client.DTrackClient;
import iabudiab.maven.plugins.dependencytrack.client.model.Project;
import iabudiab.maven.plugins.dependencytrack.client.model.ScanSubmitRequest;
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

	@Override
	protected void doWork(DTrackClient client) throws MojoExecutionException {
		Path path = Paths.get(destinationPath);
		try {
			Project project = client.getProject(projectName, projectVersion);
			client.downloadBom(project.getUuid(), path);
		} catch (IOException e) {
			throw new MojoExecutionException("Error downloading bom: ", e);
		}
	}
}
