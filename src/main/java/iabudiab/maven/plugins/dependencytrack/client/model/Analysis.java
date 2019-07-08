package iabudiab.maven.plugins.dependencytrack.client.model;

import lombok.Data;

@Data
public class Analysis {

	private State state;

	private boolean isSuppressed;
}
