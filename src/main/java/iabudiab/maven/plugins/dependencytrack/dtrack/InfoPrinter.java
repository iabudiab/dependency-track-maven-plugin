package iabudiab.maven.plugins.dependencytrack.dtrack;

import java.util.Optional;

import iabudiab.maven.plugins.dependencytrack.client.model.*;
import lombok.experimental.UtilityClass;

@UtilityClass
public class InfoPrinter {

	public static String print(ProjectMetrics metrics) {
		StringBuilder builder = new StringBuilder();
		builder.append("--- Project Metrics ---");
		appendLine(builder, "- Inherited Risk Score: ", metrics.getInheritedRiskScore());
		appendLine(builder, "- Vulnerabilities: ", metrics.getVulnerabilities());
		appendLine(builder, "- Findings Total: ", metrics.getFindingsTotal());
		appendLine(builder, "- Findings Audited: ", metrics.getFindingsAudited());
		appendLine(builder, "- Critical: ", metrics.getCritical());
		appendLine(builder, "- High: ", metrics.getHigh());
		appendLine(builder, "- Medium: ", metrics.getMedium());
		appendLine(builder, "- Low: ", metrics.getLow());
		return builder.toString();
	}

	public CharSequence print(FindingsReport report) {
		StringBuilder builder = new StringBuilder();
		builder.append("--- Findings report ---");
		builder.append("\n");

		if (report.getFindings().isEmpty()) {
			builder.append("+ Nothing to report!");
			return builder.toString();
		}

		for (Finding finding : report.getFindings()) {
			Analysis analysis = finding.getAnalysis();

			if (analysis.isSuppressed() ||
				analysis.getState() == State.NOT_AFFECTED ||
				analysis.getState() == State.FALSE_POSITIVE) {
				continue;
			}

			builder.append(print(finding));
		}

		appendLine(builder, "+ False positives : ", report.getFalsePositives());
		appendLine(builder, "+ Not affected    : ", report.getNotAffected());
		appendLine(builder, "+ Suppressed      : ", report.getSuppressed());

		return builder.toString();
	}

	public static String print(Finding finding) {
		StringBuilder builder = new StringBuilder();

		appendLine(builder, "-- Component    : ", finding.getComponent().getPurl());
		appendLine(builder, "   Vulnerability: ", finding.getVulnerability().reportSummary());
		appendLine(builder, "   Analysis     : ",
			Optional.ofNullable(finding.getAnalysis())
				.map(Analysis::getState)
				.orElse(State.NOT_SET));
		appendLine(builder, "   Suppressed   : ",
			Optional.ofNullable(finding.getAnalysis())
				.map(Analysis::isSuppressed)
				.orElse(false));
		appendLine(builder, "   Justification: ",
			Optional.ofNullable(finding.getAnalysis())
				.map(Analysis::getJustification)
				.orElse(AnalysisJustification.NOT_SET));
		appendLine(builder, "   Comment      : ",
			Optional.ofNullable(finding.getAnalysis())
				.map(Analysis::getComment)
				.orElse(""));

		builder.append("\n");

		return builder.toString();
	}

	private static void appendLine(StringBuilder builder, String title, Object value) {
		builder.append("\n");
		builder.append(title);
		builder.append(value);
	}
}
