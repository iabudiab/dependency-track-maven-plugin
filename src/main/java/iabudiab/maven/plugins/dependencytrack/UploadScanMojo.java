package iabudiab.maven.plugins.dependencytrack;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import iabudiab.maven.plugins.dependencytrack.suppressions.Suppressions;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import iabudiab.maven.plugins.dependencytrack.client.DTrackClient;
import iabudiab.maven.plugins.dependencytrack.client.model.ScanSubmitRequest;

/**
 * Mojo for uploading a
 * <a href="https://github.com/jeremylong/DependencyCheck">Dependency-Check</a>
 * report XML to <a href="https://github.com/DependencyTrack/dependency-track">Dependency-Track</a>
 * 
 * @author Iskandar Abudiab
 *
 */
@Mojo(name = "upload-scan", defaultPhase = LifecyclePhase.VERIFY, requiresOnline = true)
public class UploadScanMojo extends AbstractDependencyTrackMojo {

	/**
	 * Dependency-Check XML report directory.
	 */
	@Parameter(defaultValue = "${project.build.directory}", property = "artifactDir", required = true)
	private File artifactDirectory;

	/**
	 * Dependency-Check XML report filename.
	 */
	@Parameter(defaultValue = "dependency-check-report.xml", property = "artifactName", required = true)
	private String artifactName;

	@Override
	protected void doWork(DTrack dtrack) throws DTrackException {
		Path path = Paths.get(artifactDirectory.getPath(), artifactName);
		dtrack.uploadScan(path);
	}
}
