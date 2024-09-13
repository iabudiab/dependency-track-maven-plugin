package iabudiab.maven.plugins.dependencytrack;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.cyclonedx.model.Bom;

import iabudiab.maven.plugins.dependencytrack.cyclone.BomFormat;
import iabudiab.maven.plugins.dependencytrack.cyclone.BomUtils;
import iabudiab.maven.plugins.dependencytrack.cyclone.DiffResult;
import iabudiab.maven.plugins.dependencytrack.cyclone.DiffResultsWriter;
import iabudiab.maven.plugins.dependencytrack.cyclone.DiffUtils;
import iabudiab.maven.plugins.dependencytrack.cyclone.OutputFormat;
import iabudiab.maven.plugins.dependencytrack.dtrack.DTrack;
import iabudiab.maven.plugins.dependencytrack.dtrack.DTrackException;
import iabudiab.maven.plugins.dependencytrack.dtrack.DTrackNotFoundException;

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
	protected void logGoalConfiguration() {
		getLog().info("Diff local BOM                  : " + localBomPath);
		getLog().info("Output format                   : " + outputFormat);
		getLog().info("Output path                     : " + outputPath);
	}

	@Override
	protected void doWork(DTrack dtrack) throws MojoExecutionException {
		Bom localBom = BomUtils.readBomAtPath(Paths.get(localBomPath));
		Bom remoteBom = loadRemoteBom(dtrack);

		computeDiffAndReport(remoteBom, localBom);
	}

	private Bom loadRemoteBom(DTrack dtrack) throws MojoExecutionException {
		Path localPath = Paths.get(localBomPath);
		Path downloadDestination = localPath.resolveSibling("remote-" + localPath.getFileName());
		BomFormat format = BomUtils.probeFormat(localPath);

		try {
			dtrack.downloadBom(downloadDestination, format);
		} catch (DTrackNotFoundException e) {
			Bom emptyBom = new Bom();
			emptyBom.setComponents(new ArrayList<>());
			return emptyBom;
		} catch (DTrackException e) {
			throw new MojoExecutionException("Error downloading BOM: ", e);
		}

		return BomUtils.readBomAtPath(downloadDestination);
	}

	private void computeDiffAndReport(Bom lhs, Bom rhs) throws MojoExecutionException {
		DiffResult diffResult = DiffUtils.compute(lhs, rhs);
		DiffResultsWriter resultsWriter = new DiffResultsWriter(diffResult, outputPath, outputFormat);
		resultsWriter.write(getLog());
	}
}
