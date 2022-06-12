package iabudiab.maven.plugins.dependencytrack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import org.apache.maven.plugin.MojoExecutionException;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Utils {

	public String loadAndEncodeArtifactFile(Path path) throws MojoExecutionException {
		if (!path.toFile().exists()) {
			throw new MojoExecutionException("Could not find artifact: " + path);
		}

		try {
			return Base64.getEncoder().encodeToString(Files.readAllBytes(path));
		} catch (IOException e) {
			throw new MojoExecutionException("Error enoding artifact", e);
		}
	}
}
