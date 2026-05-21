package ftbsc.lll.gradle;

import org.gradle.api.Project;
import org.gradle.api.Plugin;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.tasks.compile.JavaCompile;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * The main class for the lillero-gradle plugin.
 */
public class LilleroGradlePlugin implements Plugin<Project> {
	private static final String PLUGIN_ID = "lillero";
	private static final String JAVA_PLUGIN_ID = "java";
	private static final String JAVA_LIBRARY_PLUGIN_ID = "java-library";
	private static final String SHADOW_PLUGIN_ID = "com.gradleup.shadow";
	private static final String SHADOW_OLD_PLUGIN_ID = "com.github.johnrengelman.shadow";

	/**
	 * The plugin ID of fabric-loom.
	 */
	public static final String LOOM_PLUGIN_ID = "fabric-loom";

	/**
	 * The plugin ID for Minecraft Forge.
	 */
	private static final String FORGE_GRADLE_PLUGIN_ID = "net.minecraftforge.gradle";

	/**
	 * The plugin ID of NeoForged.
	 */
	public static final String NEOFORGED_PLUGIN_ID = "net.neoforged.gradle.userdev";

	private static final String CORE_DEPSTRING = "ftbsc:lll:";
	private static final String PROCESSOR_DEPSTRING = "ftbsc.lll:processor:";
	private static final String MIXIN_DEPSTRING = "ftbsc.lll:mixin:";

	@Override
	public void apply(@NotNull Project proj) {
		LilleroGradleExtension extension = proj.getExtensions().create(
			PLUGIN_ID,
			LilleroGradleExtension.class,
			proj
		);

		// lillero is currently not on maven central, so we need to register a custom repo
		// in order to get the artifacts (unless the user specified otherwise)
		if(extension.getRegisterRepo().get()) {
			proj.getRepositories().maven(repo -> {
				repo.setName("zaaarf");
				repo.setUrl("https://maven.zaaarf.foo");
				repo.content(c -> {
					c.includeGroup("ftbsc");
					c.includeGroup("ftbsc.lll");
				});
			});
		}

		configureCompilerArgs(proj, extension);

		proj.afterEvaluate(project -> {
			if( // at least one of these is needed so we can add dependencies properly
				!project.getPlugins().hasPlugin(JAVA_PLUGIN_ID)
					&& !project.getPlugins().hasPlugin(JAVA_LIBRARY_PLUGIN_ID)
			) {
				return;
			}

			// bare minimum dependencies that any lillero project will need
			String coreDependency = CORE_DEPSTRING + extension.getCoreVersion().getOrElse("+");
			project.getDependencies().add("implementation", coreDependency);
			if(extension.getShadow().get()) {
				shade(project, coreDependency);
			}

			String processorDependency = PROCESSOR_DEPSTRING + extension.getProcessorVersion().getOrElse("+");
			project.getDependencies().add("compileOnly", processorDependency);
			project.getDependencies().add("annotationProcessor", processorDependency);

			if(extension.getMixinVersion().isPresent() || extension.getAuto().get() && supportsMixin(project)) {
				String mixinDependency = MIXIN_DEPSTRING + extension.getMixinVersion().getOrElse("+");
				project.getDependencies().add("implementation", mixinDependency);
				if(extension.getShadow().get()) {
					shade(project, mixinDependency);
				}
			}
		});
	}

	private static void configureCompilerArgs(Project project, LilleroGradleExtension extension) {
		project.getGradle().getTaskGraph().whenReady(graph ->
			project.getTasks().withType(JavaCompile.class).configureEach(javaCompile -> {
				List<String> compilerArgs = javaCompile.getOptions().getCompilerArgs();
				extension.getMappingsConfiguration().appendCompilerArgs(compilerArgs);
				extension.getFakeMixinConfiguration().appendCompilerArgs(compilerArgs);

				compilerArgs.add("-AanonymousClassWarning=" + extension.getAnonymousClassWarning().get());
				compilerArgs.add("-AmanualClassWarning=" + extension.getManualClassWarning().get());
				compilerArgs.add("-AobfuscateInjectorMetadata=" + extension.getObfuscateInjectorMetadata().get());
				compilerArgs.add("-AnoServiceProvider=" + extension.getNoServiceProvider().get());
				if(extension.getApiPackage().isPresent()) {
					compilerArgs.add("-AapiPackage=" + extension.getApiPackage().get());
				}
			})
		);
	}

	private static void shade(Project project, String dep) {
		if(
			project.getPlugins().hasPlugin(SHADOW_PLUGIN_ID)
				|| project.getPlugins().hasPlugin(SHADOW_OLD_PLUGIN_ID)
		) {
			Dependency shadedDep = project.getDependencies().create(dep);
			if(shadedDep instanceof ModuleDependency) {
				((ModuleDependency) shadedDep).setTransitive(false);
			}

			project.getDependencies().add("shadow", shadedDep);
		}
	}

	private static boolean supportsMixin(Project project) {
		return project.getPlugins().hasPlugin(LOOM_PLUGIN_ID) // loom always supports mixin
			|| project.getPlugins().hasPlugin(NEOFORGED_PLUGIN_ID) // neoforged always supports mixin
			|| project.getPlugins().hasPlugin(FORGE_GRADLE_PLUGIN_ID) && forgeVersionSupportsMixin(project);
	}

	// forge added mixin support in its 1.13 release (v25)
	// this is dirty but should hold as long as we Forge honors its own versioning rules
	private static boolean forgeVersionSupportsMixin(Project project) {
		Configuration minecraft = project.getConfigurations().findByName("minecraft");
		if(minecraft != null) {
			for(Dependency dep : minecraft.getDependencies()) {
				if("net.minecraftforge".equals(dep.getGroup()) && "forge".equals(dep.getName())) {
					String version = dep.getVersion();
					if(version == null) {
						return false;
					}

					int dash = version.indexOf('-');
					if(dash == -1 || dash + 1 >= version.length()) {
						return false;
					}

					String forgePart = version.substring(dash + 1);
					int dot = forgePart.indexOf('.');
					String majorString = dot == -1
						? forgePart
						: forgePart.substring(0, dot);

					try {
						return Integer.parseInt(majorString) >= 25;
					} catch (NumberFormatException e) {
						return false;
					}
				}
			}
		}

		return false;
	}
}
