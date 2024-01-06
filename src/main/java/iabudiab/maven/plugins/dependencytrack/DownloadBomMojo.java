package iabudiab.maven.plugins.dependencytrack;

import java.nio.file.Path;
import java.nio.file.Paths;

import iabudiab.maven.plugins.dependencytrack.cyclone.BomFormat;
import iabudiab.maven.plugins.dependencytrack.dtrack.DTrack;
import iabudiab.maven.plugins.dependencytrack.dtrack.DTrackException;
import iabudiab.maven.plugins.dependencytrack.dtrack.DTrackNotFoundException;
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
	protected void doWork(DTrack dtrack) throws MojoExecutionException {
		validateArguments();

		try {
			Path path = Paths.get(destinationPath);
			dtrack.downloadBom(path, outputFormat);
		} catch (DTrackNotFoundException e) {
			handleProjectNotFound(e);
		} catch (DTrackException e) {
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

	private void handleProjectNotFound(DTrackNotFoundException e) throws MojoExecutionException {
		if (failedOnNotFound) {
			throw new MojoExecutionException("Project not found: ", e);
		} else {
			getLog().info("failedOnNotFound=false, ignoring project not found: " + projectName + "-" + projectVersion);
		}
	}
}
