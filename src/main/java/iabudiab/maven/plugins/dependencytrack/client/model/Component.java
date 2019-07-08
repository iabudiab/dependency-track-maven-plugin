package iabudiab.maven.plugins.dependencytrack.client.model;

import java.util.UUID;

import lombok.Data;

@Data
public class Component {

	private UUID uuid;

	private String name;

	private String group;

	private String version;

	private String purl;
}
