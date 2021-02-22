package iabudiab.maven.plugins.dependencytrack.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectMetrics {

	private int critical;

	private int high;

	private int medium;

	private int low;

	private int unassigned;

	private long vulnerabilities;

	private int vulnerableComponents;

	private int components;

	private int suppressed;

	private int findingsTotal;

	private int findingsAudited;

	private int findingsUnaudited;

	private double inheritedRiskScore;

	private long firstOccurrence;

	private long lastOccurrence;

	public CharSequence printMetrics() {
		StringBuilder builder = new StringBuilder();
		builder.append("--- Project Metrics ---");
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
