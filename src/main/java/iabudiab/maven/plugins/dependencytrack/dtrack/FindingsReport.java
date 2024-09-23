package iabudiab.maven.plugins.dependencytrack.dtrack;

import java.util.List;

import iabudiab.maven.plugins.dependencytrack.client.model.Analysis;
import iabudiab.maven.plugins.dependencytrack.client.model.Finding;
import iabudiab.maven.plugins.dependencytrack.client.model.State;
import lombok.Getter;

@Getter
public class FindingsReport {

	private final List<Finding> findings;

	public FindingsReport(List<Finding> findings) {
		this.findings = findings;
		process();
	}

	private int falsePositives = 0;
	private int notAffected = 0;
	private int suppressed = 0;

	private void process() {
		for (Finding finding : findings) {
			Analysis analysis = finding.getAnalysis();
			checkAnalysis(analysis);
		}
	}

	private void checkAnalysis(Analysis analysis) {
		if (analysis.isSuppressed()) {
			suppressed++;
		}

		if (analysis.getState() == State.FALSE_POSITIVE) {
			falsePositives++;
		}

		if (analysis.getState() == State.NOT_AFFECTED) {
			notAffected++;
		}
	}
}
