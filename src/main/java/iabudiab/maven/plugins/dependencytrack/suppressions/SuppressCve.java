package iabudiab.maven.plugins.dependencytrack.suppressions;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonTypeName;

import iabudiab.maven.plugins.dependencytrack.client.model.AnalysisJustification;
import iabudiab.maven.plugins.dependencytrack.client.model.AnalysisResponse;
import iabudiab.maven.plugins.dependencytrack.client.model.Finding;
import iabudiab.maven.plugins.dependencytrack.client.model.State;
import lombok.Data;

@Data
@JsonTypeName("cve")
public class SuppressCve implements Suppression {

	private String type = "cve";

	private String notes;
	private State state = State.NOT_SET;
	private AnalysisJustification justification = AnalysisJustification.NOT_SET;
	private AnalysisResponse response = AnalysisResponse.NOT_SET;
	private LocalDate expiration = LocalDate.MAX;
	private String cve;

	@Override
	public boolean suppressesFinding(Finding finding) {
		return cve.equals(finding.getVulnerability().getVulnId());
	}

	@Override
	public CharSequence print() {
		StringBuilder builder = new StringBuilder();
		builder.append("- By CVE: ");
		builder.append("[").append(cve).append("]");
		builder.append(" [expired: ").append(isExpired() ? "yes": "no").append("]");
		return builder.toString();
	}

	@Override
	public CharSequence printIdentifier() {
		return "[" + cve + "]";
	}
}
