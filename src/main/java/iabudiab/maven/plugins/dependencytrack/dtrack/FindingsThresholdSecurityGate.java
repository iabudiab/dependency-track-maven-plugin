package iabudiab.maven.plugins.dependencytrack.dtrack;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import iabudiab.maven.plugins.dependencytrack.client.model.Finding;
import iabudiab.maven.plugins.dependencytrack.client.model.Severity;
import iabudiab.maven.plugins.dependencytrack.suppressions.Suppression;
import iabudiab.maven.plugins.dependencytrack.suppressions.Suppressions;
import lombok.Data;

@Data
public class FindingsThresholdSecurityGate implements SecurityGate {

	private int critical;

	private int high;

	private int medium;

	private int low;

	public static FindingsThresholdSecurityGate strict() {
		return new FindingsThresholdSecurityGate();
	}

	@Override
	public SecurityReport applyOn(List<Finding> findings, Suppressions suppressions) {
		SecurityReport securityReport = processSuppressions(findings, suppressions);

		Map<Severity, Long> statistics = securityReport.getEffectiveFindings()
			.stream()
			.collect(
				groupingBy(finding -> finding.getVulnerability().getSeverity(), counting())
			);

		if (statistics.getOrDefault(Severity.CRITICAL, 0L) > critical ||
			statistics.getOrDefault(Severity.HIGH, 0L) > high ||
			statistics.getOrDefault(Severity.MEDIUM, 0L) > medium ||
			statistics.getOrDefault(Severity.LOW, 0L) > low) {
			securityReport.fail();
		}

		return securityReport;
	}

	private SecurityReport processSuppressions(List<Finding> findings, Suppressions suppressions) {
		StringBuilder reportBuilder = new StringBuilder();
		reportBuilder.append("--- Report ---");
		reportBuilder.append("\n");

		List<Finding> effectiveFindings = new ArrayList<>();
		List<Suppression> remainingSuppression = new ArrayList<>(suppressions.getSuppressions());

		for (Finding finding : findings) {
			Suppression suppression = suppressions.suppressionFor(finding);

			if (finding.getAnalysis().isSuppressed()) {
				remainingSuppression.remove(suppression);
				reportBuilder.append("- Finding is already suppressed in Dependency-Track for: [").append(finding.getComponent().getPurl()).append("]");
				reportBuilder.append(" [cve: ").append(finding.getVulnerability().getVulnId()).append("]");
				reportBuilder.append(" [severity: ").append(finding.getVulnerability().getSeverity()).append("]");
				reportBuilder.append("\n");
				continue;
			}

			if (suppression == null) {
				effectiveFindings.add(finding);
				reportBuilder.append("- Active finding for: [").append(finding.getComponent().getPurl()).append("]");
				reportBuilder.append(" [cve: ").append(finding.getVulnerability().getVulnId()).append("]");
				reportBuilder.append(" [severity: ").append(finding.getVulnerability().getSeverity()).append("]");
				reportBuilder.append("\n");
				continue;
			}

			remainingSuppression.remove(suppression);

			if (suppression.isExpired()) {
				effectiveFindings.add(finding);
				reportBuilder.append("- Active finding with expired custom suppression for: [").append(finding.getComponent().getPurl()).append("]");
			} else {
				reportBuilder.append("- Suppressed finding via custom suppression for: [").append(finding.getComponent().getPurl()).append("]");
			}

			reportBuilder.append(" [cve: ").append(finding.getVulnerability().getVulnId()).append("]");
			reportBuilder.append(" [severity: ").append(finding.getVulnerability().getSeverity()).append("]");

			if (suppression.getNotes() != null) {
				reportBuilder.append(" [notes: ").append(suppression.getNotes()).append("]");
			}

			String expiration = suppression.getExpiration().equals(LocalDate.MAX)
				? "never expires"
				: suppression.getExpiration().toString();

			reportBuilder.append(" [suppression expiration date: ").append(expiration).append("]");
			reportBuilder.append("\n");
		}

		for (Suppression suppression : remainingSuppression) {
			reportBuilder.append("- Unnecessary suppression for: ").append(suppression.printIdentifier()).append("\n");
		}

		List<Suppression> effectiveSuppressions = new ArrayList<>(suppressions.getSuppressions());
		effectiveSuppressions.removeAll(remainingSuppression);

		return new SecurityReport(true, reportBuilder.toString(), effectiveFindings, effectiveSuppressions);
	}

	public CharSequence print() {
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
