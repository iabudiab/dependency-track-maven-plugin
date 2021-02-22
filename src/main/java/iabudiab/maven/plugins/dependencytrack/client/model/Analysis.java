package iabudiab.maven.plugins.dependencytrack.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Analysis {

	private State state;

	@JsonProperty("isSuppressed")
	private boolean suppressed;
}
