package iabudiab.maven.plugins.dependencytrack.cyclone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;

public class DiffUtils {

	public static DiffResult compute(Bom from, Bom to) {
		Map<String, DiffItem<Component>> result = new HashMap<>();

		List<Component> fromComponents = new ArrayList<>(from.getComponents());
		List<Component> toComponents = new ArrayList<>(to.getComponents());

		for (Component fromComponent : from.getComponents()) {
			long count = toComponents.stream()
					.filter(toComponent -> toComponent.getGroup().equals(fromComponent.getGroup()) &&
							toComponent.getName().equals(fromComponent.getName()) &&
							toComponent.getVersion().equals(fromComponent.getVersion()))
					.count();

			if (count > 0) {
				String identifier = identifier(fromComponent);

				if (!result.containsKey(identifier)) {
					result.put(identifier, new DiffItem<>());
				}

				result.get(identifier).getUnchanged().add(fromComponent);

				fromComponents.removeIf(component -> component.getGroup().equals(fromComponent.getGroup()) &&
						component.getName().equals(fromComponent.getName())
						&& component.getVersion().equals(fromComponent.getVersion()));

				toComponents.removeIf(component -> component.getGroup().equals(fromComponent.getGroup()) &&
						component.getName().equals(fromComponent.getName())
						&& component.getVersion().equals(fromComponent.getVersion()));
			}
		}

		for (Component component : toComponents) {
			String identifier = identifier(component);

			if (!result.containsKey(identifier)) {
				result.put(identifier, new DiffItem<>());
			}

			result.get(identifier).getAdded().add(component);
		}

		for (Component component : fromComponents) {
			String identifier = identifier(component);

			if (!result.containsKey(identifier)) {
				result.put(identifier, new DiffItem<>());
			}

			result.get(identifier).getRemoved().add(component);
		}

		return new DiffResult(result);
	}

	private static String identifier(Component component) {
		String identifier = component.getGroup() + ":" + component.getName();
		if (identifier.startsWith(":")) {
			identifier = identifier.substring(1);
		}
		return identifier;
	}
}
