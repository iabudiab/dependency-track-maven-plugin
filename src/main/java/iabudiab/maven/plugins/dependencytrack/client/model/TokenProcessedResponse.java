package iabudiab.maven.plugins.dependencytrack.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TokenProcessedResponse {

	private boolean processing;
}
