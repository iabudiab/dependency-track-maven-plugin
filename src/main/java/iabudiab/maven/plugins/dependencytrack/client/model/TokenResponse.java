package iabudiab.maven.plugins.dependencytrack.client.model;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.apache.maven.plugin.MojoExecutionException;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TokenResponse {

	private UUID token;
}
