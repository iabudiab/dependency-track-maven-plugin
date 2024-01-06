package iabudiab.maven.plugins.dependencytrack.dtrack;

public class DTrackNotFoundException extends DTrackException {

	public DTrackNotFoundException(String message) {
		super(message);
	}

	public DTrackNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}
}
