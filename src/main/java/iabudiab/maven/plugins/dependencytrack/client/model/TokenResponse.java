package iabudiab.maven.plugins.dependencytrack.client.model;

import java.util.UUID;

import lombok.Value;

@Value
public class TokenResponse {

	private UUID token;
}
