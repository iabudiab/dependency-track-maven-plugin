package iabudiab.maven.plugins.dependencytrack.suppressions;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonTypeName;

import iabudiab.maven.plugins.dependencytrack.client.model.AnalysisJustification;
import iabudiab.maven.plugins.dependencytrack.client.model.AnalysisResponse;
import iabudiab.maven.plugins.dependencytrack.client.model.Finding;
import iabudiab.maven.plugins.dependencytrack.client.model.State;
import lombok.Data;

@Data
@JsonTypeName("cve-of-purl")
public class SuppressCveOfPurl implements Suppression {

	private String type = "cve-of-purl";

	private String notes;
	private State state = State.NOT_SET;
	private AnalysisJustification justification = AnalysisJustification.NOT_SET;
	private AnalysisResponse response = AnalysisResponse.NOT_SET;
	private LocalDate expiration = LocalDate.MAX;
	private String purl;
	private String cve;

	private boolean regex = false;

	@Override
	public boolean suppressesFinding(Finding finding) {
		if (!cve.equals(finding.getVulnerability().getVulnId())) {
			return false;
		}

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
		builder.append("- By CVE: ");
		builder.append(cve);

		builder.append(" of PURL");
		if (regex) {
			builder.append(" [exact match]: ");
		} else {
			builder.append(" [as regex]: ");
		}

		builder.append("[").append(purl).append("]");
		builder.append(" [Expired: ").append(isExpired() ? "yes": "no").append("]");
		return builder.toString();
	}
}
