package iabudiab.maven.plugins.dependencytrack;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import iabudiab.maven.plugins.dependencytrack.client.DTrackClient;
import iabudiab.maven.plugins.dependencytrack.client.model.Project;
import iabudiab.maven.plugins.dependencytrack.cyclone.BomUtils;
import iabudiab.maven.plugins.dependencytrack.cyclone.DiffResult;
import iabudiab.maven.plugins.dependencytrack.cyclone.DiffResultsWriter;
import iabudiab.maven.plugins.dependencytrack.cyclone.DiffUtils;
import org.apache.http.client.HttpResponseException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.cyclonedx.model.Bom;

/**
 * Mojo for building a Diff between a local BOM and the corresponding BOM in Dependency Track.
 *
 * @author Iskandar Abudiab
 *
 */
@Mojo(name = "diff-dependency-track", defaultPhase = LifecyclePhase.VERIFY, requiresOnline = true)
public class DiffWithDependencyTrackBomMojo extends AbstractDependencyTrackMojo {

	/**
	 * The local file path for the BOM artifact.
	 */
	@Parameter(defaultValue = "${project.build.directory}/bom.xml", property = "localBomPath", required = true)
	private String localBomPath;

	/**
	 * The output format of the generated diff.
	 */
	@Parameter(defaultValue = "JSON", property = "outputFormat", required = true)
	private OutputFormat outputFormat;

	/**
	 * Path for the output file with diff results.
	 */
	@Parameter(defaultValue = "${project.build.directory}/diff.json", property = "outputFile", required = true)
	private String outputPath;

	@Override
	protected void doWork(DTrackClient client) throws MojoExecutionException, MojoFailureException {
		Bom localBom = BomUtils.readBomAtPath(Paths.get(localBomPath));
		Bom remoteBom = loadRemoteBom(client);

		computeDiffAndReport(remoteBom, localBom);
	}

	private Bom loadRemoteBom(DTrackClient client) throws MojoExecutionException {
		Project project;
		try {
			project = client.getProject(projectName, projectVersion);
		} catch (HttpResponseException e) {
			if (e.getStatusCode() == 404) {
				Bom emptyBom = new Bom();
				emptyBom.setComponents(new ArrayList<>());
				return emptyBom;
			} else {
				throw new MojoExecutionException("Error loading project: ", e);
			}
		} catch (IOException e) {
			throw new MojoExecutionException("Error loading project: ", e);
		}

		Path localPath = Paths.get(localBomPath);
		Path downloadDestination = localPath.resolveSibling("remote-" + localPath.getFileName());
		BomFormat format = BomUtils.probeFormat(localPath);

		try {
			client.downloadBom(project.getUuid(), downloadDestination, format);
		} catch (IOException e) {
			throw new MojoExecutionException("Error downloading bom: ", e);
		}

		return BomUtils.readBomAtPath(downloadDestination);
	}

	private void computeDiffAndReport(Bom lhs, Bom rhs) throws MojoExecutionException {
		DiffResult diffResult = DiffUtils.compute(lhs, rhs);
		DiffResultsWriter resultsWriter = new DiffResultsWriter(diffResult, outputPath, outputFormat);
		resultsWriter.write(getLog());
	}
}
