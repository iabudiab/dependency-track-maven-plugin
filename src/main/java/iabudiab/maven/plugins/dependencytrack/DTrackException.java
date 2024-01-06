package iabudiab.maven.plugins.dependencytrack;

public class DTrackException extends RuntimeException {

	public DTrackException(String message) {
		super(message);
	}

	public DTrackException(String message, Throwable cause) {
		super(message, cause);
	}
}
