package iabudiab.maven.plugins.dependencytrack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Utils {

	protected String loadAndEncodeArtifactFile(Path path) throws PluginException {
		if (!path.toFile().exists()) {
			throw new PluginException("Could not find artifact: " + path);
		}

		try {
			return Base64.getEncoder().encodeToString(Files.readAllBytes(path));
		} catch (IOException e) {
			throw new PluginException("Error enoding artifact", e);
		}
	}
}
