package iabudiab.maven.plugins.dependencytrack.cyclone;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.cyclonedx.model.Component;

@Data
@AllArgsConstructor
public class DiffResult {

	private Map<String, DiffItem<Component>> componentVersions;

	public boolean hasChanges() {
		return componentVersions.entrySet().stream().anyMatch(entry -> entry.getValue().hasChanges());
	}
}
