package ftbsc.lll.gradle.util;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;

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

	/**
	 * Create the dependency provider from the blueprint.
	 * @return the provider
	 */
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

	/**
	 * Create a dependency provider from the given parameters
	 * @param project the project
	 * @param depstring the depstring
	 * @param version the version provider
	 * @return the provider
	 */
	public static Provider<Dependency> dependency(Project project, String depstring, Property<String> version) {
		return version.orElse("+").map(v -> project.getDependencies().create(depstring + v));
	}

	/**
	 * A condition for creating a dependency.
	 */
	@FunctionalInterface
	public interface ApplyCondition {
		/**
		 * The actual condition.
		 * @param project the project
		 * @param depstring the depstring
		 * @param versionProvider the version provider
		 * @return whether it should be applied
		 */
		boolean should(Project project, String depstring, Provider<String> versionProvider);
	}
}
