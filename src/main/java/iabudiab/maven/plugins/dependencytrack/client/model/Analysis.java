package iabudiab.maven.plugins.dependencytrack.client.model;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Analysis {

	@JsonProperty("project")
	private UUID projectUuid;

	@JsonProperty("component")
	private UUID componentUuid;

	@JsonProperty("vulnerability")
	private UUID vulnerabilityUuid;

	@JsonProperty("analysisState")
	private State state;

	@JsonProperty("analysisJustification")
	private AnalysisJustification justification;

	@JsonProperty("analysisResponse")
	private AnalysisResponse response;

	@JsonProperty("comment")
	private String comment;

	@JsonProperty("isSuppressed")
	private boolean suppressed;

}
