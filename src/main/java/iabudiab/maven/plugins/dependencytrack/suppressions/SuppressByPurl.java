package iabudiab.maven.plugins.dependencytrack.suppressions;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonTypeName;

import iabudiab.maven.plugins.dependencytrack.client.model.AnalysisJustification;
import iabudiab.maven.plugins.dependencytrack.client.model.AnalysisResponse;
import iabudiab.maven.plugins.dependencytrack.client.model.Finding;
import iabudiab.maven.plugins.dependencytrack.client.model.State;

@JsonTypeName("purl")
public class SuppressByPurl implements Suppression {

	private final String type = "purl";

	private String notes;
	private final State state = State.NOT_SET;
	private final AnalysisJustification justification = AnalysisJustification.NOT_SET;
	private final AnalysisResponse response = AnalysisResponse.NOT_SET;
	private final LocalDate expiration = LocalDate.MAX;
	private String purl;

	private final boolean regex = false;

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
		if (regex) {
			Pattern pattern = Pattern.compile(purl);
			Matcher matcher = pattern.matcher(finding.getComponent().getPurl());
			return matcher.find();
		} else {
			return purl.equals(finding.getComponent().getPurl());
		}
	}

	@Override
	public CharSequence print() {
		StringBuilder builder = new StringBuilder();
		builder.append("- By PURL");
		if (regex) {
			builder.append(" [exact match]: ");
		} else {
			builder.append(" [as regex]: ");
		}

		builder.append("[").append(purl).append("]");
		builder.append(" [expired: ").append(isExpired() ? "yes": "no").append("]");
		return builder.toString();
	}

	@Override
	public CharSequence printIdentifier() {
		return "[" + purl + "]";
	}
}
