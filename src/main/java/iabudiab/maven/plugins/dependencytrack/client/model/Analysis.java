package iabudiab.maven.plugins.dependencytrack.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class Analysis {

	private State state;

	@JsonProperty("isSuppressed")
	private boolean suppressed;
}
