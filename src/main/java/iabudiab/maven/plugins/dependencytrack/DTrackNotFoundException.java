package iabudiab.maven.plugins.dependencytrack;

public class DTrackNotFoundException extends DTrackException {

	public DTrackNotFoundException(String message) {
		super(message);
	}

	public DTrackNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}
}
