package iabudiab.maven.plugins.dependencytrack.suppressions;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonTypeName;
import iabudiab.maven.plugins.dependencytrack.client.model.Finding;
import lombok.Data;

@Data
@JsonTypeName("purl")
public class SuppressByPurl implements Suppression {

	private String type = "purl";

	private String notes;
	private LocalDate expiration = LocalDate.MAX;
	private String purl;

	private boolean regex = false;

	@Override
	public boolean shouldSuppress(Finding finding) {
		if (isExpired()) {
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
}
