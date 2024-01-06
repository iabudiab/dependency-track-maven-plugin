package iabudiab.maven.plugins.dependencytrack.cyclone;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.AllArgsConstructor;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.cyclonedx.model.Component;

@AllArgsConstructor
public class DiffResultsWriter {

	private DiffResult diff;
	private String outputPath;
	private OutputFormat outputFormat;

	public void write(Log log) throws MojoExecutionException {
		switch (outputFormat) {
			case JSON:
				writeJson(log);
			case TEXT:
				writeText(log);
		}
	}

	private void writeJson(Log log) throws MojoExecutionException {
		log.info("Writing diff result JSON at :" + outputPath);
		ObjectMapper mapper = new ObjectMapper();
		ObjectWriter prettyPrinter = mapper.writerWithDefaultPrettyPrinter();

		try {
			Path path = Paths.get(outputPath);
			prettyPrinter.writeValue(Files.newOutputStream(path.toFile().toPath()), diff);
		} catch (IOException e) {
			throw new MojoExecutionException("Error writing diff results at path: " + outputPath, e);
		}
	}

	private void writeText(Log log) {
		log.info("Component versions that have changed:");
		log.info("");

		if (diff.hasChanges()) {
			for (Map.Entry<String, DiffItem<Component>> entry : diff.getComponentVersions().entrySet()) {
				DiffItem<Component> item = entry.getValue();
				if (!item.hasChanges()) {
					continue;
				}

				for (Component component : item.getRemoved()) {
					log.info(String.format("- %s %s @ %s", component.getGroup(), component.getName(), component.getVersion()));
				}

				for (Component component : item.getUnchanged()) {
					log.info(String.format("= %s %s @ %s", component.getGroup(), component.getName(), component.getVersion()));
				}

				for (Component component : item.getAdded()) {
					log.info(String.format("+ %s %s @ %s", component.getGroup(), component.getName(), component.getVersion()));
				}
				log.info("");
			}
		} else {
			log.info("None.");
		}
	}
}
