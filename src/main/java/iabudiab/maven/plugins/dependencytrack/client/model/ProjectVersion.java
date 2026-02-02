package iabudiab.maven.plugins.dependencytrack.client.model;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectVersion {
	
	private UUID uuid;
	private String version;
	private Boolean active;

}
