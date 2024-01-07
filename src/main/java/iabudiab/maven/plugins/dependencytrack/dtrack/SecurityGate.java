package iabudiab.maven.plugins.dependencytrack.dtrack;

import java.util.List;

import iabudiab.maven.plugins.dependencytrack.client.model.Finding;
import iabudiab.maven.plugins.dependencytrack.suppressions.Suppressions;

public interface SecurityGate {

	SecurityGateDecision checkAgainst(List<Finding> findings, Suppressions suppressions);
}
