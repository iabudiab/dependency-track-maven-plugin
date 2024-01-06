package iabudiab.maven.plugins.dependencytrack.dtrack;

public class DTrackUnauthenticatedException extends DTrackException {

	public DTrackUnauthenticatedException(String message) {
		super(message);
	}

	public DTrackUnauthenticatedException(String message, Throwable cause) {
		super(message, cause);
	}
}
