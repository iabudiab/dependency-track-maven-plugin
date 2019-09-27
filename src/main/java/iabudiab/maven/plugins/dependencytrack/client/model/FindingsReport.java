package iabudiab.maven.plugins.dependencytrack.client.model;

import java.util.List;

public class FindingsReport {

	private List<Finding> findings;

	public FindingsReport(List<Finding> findings) {
		this.findings = findings;
	}

	private int falsePositives = 0;
	private int notAffected = 0;
	private int suppressed = 0;

	public CharSequence printSummary() {
		StringBuilder builder = new StringBuilder();
		builder.append("--- Findings report ---");
		builder.append("\n");
		if (findings.isEmpty()) {
			builder.append("+ Nothing to report!");
			return builder.toString();
		}

		for (Finding finding : findings) {
			Analysis analysis = finding.getAnalysis();
			if (checkAnalysis(analysis)) {
				continue;
			}

			builder.append("-  Component    : " + finding.getComponent().getPurl());
			builder.append("\n");
			builder.append("   Vulnerability: " + finding.getVulnerability().reportSummary());
			builder.append("\n");
			builder.append("   Analysis     : " + finding.getAnalysis().getState());
			builder.append("\n");
		}

		builder.append("+ False positives : " + falsePositives);
		builder.append("\n");
		builder.append("+ Not affected    : " + notAffected);
		builder.append("\n");
		builder.append("+ Suppressed      : " + suppressed);
		builder.append("\n");

		return builder.toString();
	}

	private boolean checkAnalysis(Analysis analysis) {
		boolean shouldIgnore = false;

		if (analysis.isSuppressed()) {
			suppressed++;
			shouldIgnore = true;
		}

		if (analysis.getState() == State.FALSE_POSITIVE) {
			falsePositives++;
			shouldIgnore = true;
		}

		if (analysis.getState() == State.NOT_AFFECTED) {
			notAffected++;
			shouldIgnore = true;
		}

		return shouldIgnore;
	}
}
