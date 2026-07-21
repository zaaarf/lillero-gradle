package ftbsc.lll.gradle.util;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Wrapper around Provider that can be recreated on demand.
 * Needed because each Provider must be used once, apparently.
 */
@AllArgsConstructor
@RequiredArgsConstructor
public class DependencyBlueprint {
	private final Project project;
	private final String depstring;
	private final Property<String> version;
	private ApplyCondition condition = null;

	public Provider<Dependency> build() {
		if(this.condition == null) {
			return dependency(this.project, this.depstring, this.version);
		} else {
			return this.project.provider(() -> {
				if(this.condition.should(this.project, this.depstring, this.version)) {
					return this.project.getDependencies().create(this.depstring + this.version.getOrElse("+"));
				}

				return null;
			});
		}
	}

	public static Provider<Dependency> dependency(Project project, String depstring, Property<String> version) {
		return version.orElse("+").map(v -> project.getDependencies().create(depstring + v));
	}

	@FunctionalInterface
	public interface ApplyCondition {
		boolean should(Project project, String depstring, Provider<String> versionProvider);
	}
}
