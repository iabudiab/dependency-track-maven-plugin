package iabudiab.maven.plugins.dependencytrack;

import java.util.List;

import iabudiab.maven.plugins.dependencytrack.client.model.Finding;
import iabudiab.maven.plugins.dependencytrack.suppressions.Suppressions;

public interface SecurityGate {

	SecurityReport applyOn(List<Finding> findings, Suppressions suppressions);
}
