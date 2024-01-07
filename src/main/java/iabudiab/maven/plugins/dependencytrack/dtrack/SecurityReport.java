package iabudiab.maven.plugins.dependencytrack.dtrack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import iabudiab.maven.plugins.dependencytrack.client.model.Finding;
import iabudiab.maven.plugins.dependencytrack.suppressions.Suppression;
import iabudiab.maven.plugins.dependencytrack.suppressions.Suppressions;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

@Data
@AllArgsConstructor
public class SecurityReport {

	private String details;
	private List<Finding> effectiveFindings;
	private List<Suppression> effectiveSuppressions;

	public void cleanupSuppressionsFile(Log log, String suppressionsFile) throws MojoExecutionException {
		try {
			Path targetSuppressionsFilePath = Paths.get(suppressionsFile);
			Files.createDirectories(targetSuppressionsFilePath.getParent());

			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.registerModule(new JavaTimeModule());

			byte[] filteredSuppressionsBytes =
				objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(new Suppressions(effectiveSuppressions));

			Files.write(
				targetSuppressionsFilePath,
				filteredSuppressionsBytes,
				StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING,
				StandardOpenOption.WRITE
			);
			log.info("Effective suppressions have been written to: " + targetSuppressionsFilePath);
		} catch (IOException e) {
			throw new MojoExecutionException("Error writing suppressions: ", e);
		}
	}
}
