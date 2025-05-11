package iabudiab.maven.plugins.dependencytrack.cyclone;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DiffItem<T> {

	private List<T> added;
	private List<T> removed;
	private List<T> unchanged;

	public DiffItem() {
		this.added = new ArrayList<>();
		this.removed = new ArrayList<>();
		this.unchanged = new ArrayList<>();
	}

	public boolean hasChanges() {
		return !added.isEmpty() || !removed.isEmpty();
	}
}
