package iabudiab.maven.plugins.dependencytrack.suppressions;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonTypeName;

import iabudiab.maven.plugins.dependencytrack.client.model.AnalysisJustification;
import iabudiab.maven.plugins.dependencytrack.client.model.AnalysisResponse;
import iabudiab.maven.plugins.dependencytrack.client.model.Finding;
import iabudiab.maven.plugins.dependencytrack.client.model.State;

@JsonTypeName("cve")
public class SuppressCve implements Suppression {

	private final String type = "cve";

	private String notes;
	private final State state = State.NOT_SET;
	private final AnalysisJustification justification = AnalysisJustification.NOT_SET;
	private final AnalysisResponse response = AnalysisResponse.NOT_SET;
	private final LocalDate expiration = LocalDate.MAX;
	private String cve;

	@Override
	public String getType() {
		return type;
	}

	@Override
	public String getNotes() {
		return notes;
	}

	@Override
	public State getState() {
		return state;
	}

	@Override
	public AnalysisJustification getJustification() {
		return justification;
	}

	@Override
	public AnalysisResponse getResponse() {
		return response;
	}

	@Override
	public LocalDate getExpiration() {
		return expiration;
	}

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
