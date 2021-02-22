package iabudiab.maven.plugins.dependencytrack.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Finding {

	private Component component;

	private Vulnerability vulnerability;

	private Analysis analysis;

	private String matrix;

}
