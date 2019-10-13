package iabudiab.maven.plugins.dependencytrack.client.model;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Project {

	private UUID uuid;
	private String name;
	private String version;
}
