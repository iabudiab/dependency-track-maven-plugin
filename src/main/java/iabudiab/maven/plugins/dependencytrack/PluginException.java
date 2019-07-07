package iabudiab.maven.plugins.dependencytrack;

public class PluginException extends RuntimeException {

	private static final long serialVersionUID = -2707081916705610267L;

	public PluginException(String msg) {
		super(msg);
	}

	public PluginException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
