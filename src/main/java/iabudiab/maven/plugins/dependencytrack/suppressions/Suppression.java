package iabudiab.maven.plugins.dependencytrack.suppressions;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.*;

import iabudiab.maven.plugins.dependencytrack.client.model.AnalysisJustification;
import iabudiab.maven.plugins.dependencytrack.client.model.AnalysisResponse;
import iabudiab.maven.plugins.dependencytrack.client.model.Finding;
import iabudiab.maven.plugins.dependencytrack.client.model.State;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "by", include = JsonTypeInfo.As.EXISTING_PROPERTY, visible = true)
@JsonSubTypes({
	@JsonSubTypes.Type(value = SuppressByPurl.class, name = "purl"),
	@JsonSubTypes.Type(value = SuppressCve.class, name = "cve"),
	@JsonSubTypes.Type(value = SuppressCveOfPurl.class, name = "cve-of-purl"),
})
public interface Suppression {

	@JsonProperty("by")
	String getType();

	String getNotes();

	State getState();

	AnalysisJustification getJustification();

	AnalysisResponse getResponse();

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	LocalDate getExpiration();

	boolean suppressesFinding(Finding finding);

	default boolean shouldSuppress(Finding finding) {
		return isNotExpired() && suppressesFinding(finding);
	}

	default boolean isExpired() {
		return LocalDate.now().isAfter(getExpiration());
	}

	default boolean isNotExpired() {
		return LocalDate.now().isBefore(getExpiration());
	}

	CharSequence print();

	CharSequence printIdentifier();
}
