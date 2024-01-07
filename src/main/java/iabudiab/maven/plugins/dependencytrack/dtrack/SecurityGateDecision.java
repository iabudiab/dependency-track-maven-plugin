package iabudiab.maven.plugins.dependencytrack.dtrack;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.maven.plugin.logging.Log;

@Getter
@AllArgsConstructor
public class SecurityGateDecision {

	public enum Decision {
		PASS, FAIL
	}

	private Decision decision;
	private SecurityReport report;

	public void fail() {
		decision = Decision.FAIL;
	}

	public void execute(Log log) throws SecurityGateRejectionException {
		log.info(report.getDetails());

		if (decision == Decision.FAIL) {
			log.warn("Project did not pass the Security Gate");
			throw new SecurityGateRejectionException("Project did not pass the Security Gate");
		}
	}
}
