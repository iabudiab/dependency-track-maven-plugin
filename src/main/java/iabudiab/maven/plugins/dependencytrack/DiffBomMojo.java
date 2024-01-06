package iabudiab.maven.plugins.dependencytrack;

import java.nio.file.Paths;

import iabudiab.maven.plugins.dependencytrack.cyclone.BomUtils;
import iabudiab.maven.plugins.dependencytrack.cyclone.DiffResult;
import iabudiab.maven.plugins.dependencytrack.cyclone.DiffResultsWriter;
import iabudiab.maven.plugins.dependencytrack.cyclone.OutputFormat;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Mojo for building a Diff between two BOMs.
 *
 * @author Iskandar Abudiab
 *
 */
@Mojo(name = "diff", defaultPhase = LifecyclePhase.VERIFY, requiresOnline = false)
public class DiffBomMojo extends AbstractMojo {

	/**
	 * The diff-from BOM file.
	 */
	@Parameter(property = "from", required = true)
	private String from;

	/**
	 * The diff-to BOM file.
	 */
	@Parameter(property = "to", required = true)
	private String to;

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
	public void execute() throws MojoExecutionException {
		DiffResult diffResult = BomUtils.computeDiff(Paths.get(from), Paths.get(to));

		DiffResultsWriter resultsWriter = new DiffResultsWriter(diffResult, outputPath, outputFormat);
		resultsWriter.write(getLog());
	}
}
