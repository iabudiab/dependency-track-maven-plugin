package iabudiab.maven.plugins.dependencytrack;

import org.apache.maven.plugin.MojoFailureException;

public class SecurityGateRejectionException extends MojoFailureException {

	private static final long serialVersionUID = -3395152761184106135L;

	public SecurityGateRejectionException(String msg) {
		super(msg);
	}
}
