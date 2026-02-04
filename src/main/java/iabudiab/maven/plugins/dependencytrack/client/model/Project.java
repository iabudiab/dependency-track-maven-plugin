package iabudiab.maven.plugins.dependencytrack.client.model;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Project {

	private UUID uuid;
	private String name;
	private String version;
	private Project parent;
	private Classifier classifier;
	private CollectionLogic collectionLogic;
	private Tag collectionTag;
	private Boolean isLatest;
	private Boolean active;
	private List<ProjectVersion> versions;

	public Project() {
		classifier = Classifier.NONE;
		collectionLogic = CollectionLogic.NONE;
		active = true;
		isLatest = false;
	}
}
