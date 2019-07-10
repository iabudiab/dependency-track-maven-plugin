package iabudiab.maven.plugins.dependencytrack;

import iabudiab.maven.plugins.dependencytrack.client.model.ProjectMetrics;
import lombok.Data;

@Data
public class SecurityGate {

	private int critical;

	private int high;

	private int medium;

	private int low;

	public static SecurityGate strict() {
		return new SecurityGate();
	}

	public void applyOn(ProjectMetrics projectMetrics) throws SecurityGateRejectionException {
		if (critical >= projectMetrics.getCritical() || //
				high >= projectMetrics.getHigh() || //
				medium >= projectMetrics.getMedium() || //
				low >= projectMetrics.getLow()) {
			throw new SecurityGateRejectionException("Project did not pass the Security Gate");
		}
	}

	public CharSequence printThresholds() {
		StringBuilder builder = new StringBuilder();
		builder.append("--- Security Gate ---");
		builder.append("\n");
		builder.append("critical: ");
		builder.append(critical);
		builder.append(", high: ");
		builder.append(high);
		builder.append(", medium: ");
		builder.append(medium);
		builder.append(", low: ");
		builder.append(low);
		return builder.toString();
	}
}
