package iabudiab.maven.plugins.dependencytrack.client.model;

public enum State {
	NOT_SET,
	NOT_AFFECTED,
	IN_TRIAGE,
	EXPLOITABLE,
	FALSE_POSITIVE,
	RESOLVED
}
