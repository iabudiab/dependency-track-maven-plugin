package iabudiab.maven.plugins.dependencytrack.client.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BomSubmitRequest {

	private String project;

	private String projectName;

	private String projectVersion;

	private String parentName;

	private String parentVersion;

	private Boolean autoCreate;

	private String bom;

}
