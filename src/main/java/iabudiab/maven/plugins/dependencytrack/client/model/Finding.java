package iabudiab.maven.plugins.dependencytrack.client.model;

import lombok.Data;

@Data
public class Finding {

	private Component component;

	private Vulnerability vulnerability;

	private Analysis analysis;

	private String matrix;

}
