package iabudiab.maven.plugins.dependencytrack.suppressions;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import iabudiab.maven.plugins.dependencytrack.client.model.Finding;
import lombok.Data;


@Data
public class Suppressions {

	private List<Suppression> suppressions;

	public static Suppressions none() {
		return new Suppressions(new ArrayList<>());
	}

	public Suppressions(@JsonProperty("suppressions") List<Suppression> suppressions) {
		this.suppressions = suppressions;
	}

	public boolean hasSuppressions() {
		return !isEmpty();
	}

	public boolean isEmpty() {
		return suppressions.isEmpty();
	}

	public boolean shouldSuppress(Finding finding) {
		return suppressions.stream()
			.anyMatch(suppression -> suppression.shouldSuppress(finding));
	}

	public Suppression suppressionFor(Finding finding) {
		for (Suppression suppression : suppressions) {
			if (suppression.suppressesFinding(finding)) {
				return suppression;
			}
		}
		return null;
	}

	public CharSequence print() {
		StringBuilder builder = new StringBuilder();
		builder.append("--- Custom Suppressions ---");
		builder.append("\n");
		if (suppressions.isEmpty()) {
			builder.append("- None");
		} else {
			for (Suppression suppression : suppressions) {
				builder.append(suppression.print());
				builder.append("\n");
			}
		}
		return builder.toString();
	}
}
