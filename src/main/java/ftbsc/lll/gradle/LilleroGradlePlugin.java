package ftbsc.lll.gradle;

import ftbsc.lll.gradle.exceptions.MappingFetchException;
import ftbsc.lll.gradle.util.MappingsConfiguration;
import net.fabricmc.loom.api.LoomGradleExtensionAPI;
import org.gradle.api.Project;
import org.gradle.api.Plugin;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.tasks.compile.JavaCompile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

/**
 * The main class for the lillero-gradle plugin.
 */
public class LilleroGradlePlugin implements Plugin<Project> {
	public static final String PLUGIN_ID = "lillero";
	public static final String JAVA_PLUGIN_ID = "java";
	public static final String JAVA_LIBRARY_PLUGIN_ID = "java-library";
	public static final String LOOM_PLUGIN_ID = "fabric-loom";
	public static final String FORGE_GRADLE_PLUGIN_ID = "net.minecraftforge.gradle";
	public static final String SHADOW_PLUGIN_ID = "com.gradleup.shadow";
	public static final String SHADOW_OLD_PLUGIN_ID = "com.github.johnrengelman.shadow";

	private static final String CORE_DEPSTRING = "ftbsc:lll:";
	private static final String PROCESSOR_DEPSTRING = "ftbsc.lll:processor:";
	private static final String MIXIN_DEPSTRING = "ftbsc.lll:mixin";
	private static final String LOADER_DEPSTRING = "ftbsc.lll:loader";

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
			project.getDependencies().add("implementation", CORE_DEPSTRING + extension.getCoreVersion().get());
			if(extension.getShadow().get()) {
				shade(project, CORE_DEPSTRING + extension.getCoreVersion().get());
			}

			project.getDependencies().add("implementation", PROCESSOR_DEPSTRING + extension.getCoreVersion().get());
			project.getDependencies().add("annotationProcessor", PROCESSOR_DEPSTRING + extension.getCoreVersion().get());

			// if auto-configure, add the appropriate loader
			if(extension.getAuto().get()) {
				if(proj.getPlugins().hasPlugin(LilleroGradlePlugin.LOOM_PLUGIN_ID)) {
					proj.getDependencies().add("implementation", MIXIN_DEPSTRING + extension.getCoreVersion().get());
					if(extension.getShadow().get()) {
						shade(project, CORE_DEPSTRING + extension.getCoreVersion().get());
					}
				} else if(proj.getPlugins().hasPlugin(LilleroGradlePlugin.FORGE_GRADLE_PLUGIN_ID)) {
					proj.getDependencies().add("implementation", LOADER_DEPSTRING + extension.getCoreVersion().get());
				}
			}
		});

		// handle loom-specific setup
		proj.getPlugins().withId(LOOM_PLUGIN_ID, applied -> {
			LoomGradleExtensionAPI loomExt = proj.getExtensions().findByType(LoomGradleExtensionAPI.class);
			if(loomExt == null) { // loom applied but extension not found?
				return;
			}

			// generate loom mappings
			MappingsConfiguration configuration = extension.getMappingsConfiguration();
			if(!configuration.isOverride()) {
				try {
					//noinspection UnstableApiUsage
					loomExt.getIntermediateMappingsProvider().provide(configuration.getMappings());
				} catch(IOException e) {
					throw new MappingFetchException(LOOM_PLUGIN_ID, e);
				}
			}
		});
	}

	private static void configureCompilerArgs(
		Project project,
		LilleroGradleExtension extension
	) {
		project.getTasks().withType(JavaCompile.class).configureEach(javaCompile -> {
			List<String> compilerArgs = javaCompile.getOptions().getCompilerArgs();
			extension.getMappingsConfiguration().appendCompilerArgs(compilerArgs);
			extension.getFakeMixinConfiguration().appendCompilerArgs(compilerArgs);

			compilerArgs.add("-AanonymousClassWarning=" + extension.getAnonymousClassWarning().get());
			compilerArgs.add("-AmanualClassWarning=" + extension.getManualClassWarning().get());
			compilerArgs.add("-AobfuscateInjectorMetadata=" + extension.getObfuscateInjectorMetadata().get());
			compilerArgs.add("-AnoServiceProvider=" + extension.getNoServiceProvider().get());
		});
	}

	private static void shade(Project project, String dep) {
		if(project.getPlugins().hasPlugin(SHADOW_PLUGIN_ID) || project.getPlugins().hasPlugin(SHADOW_OLD_PLUGIN_ID)) {
			Dependency shadedDep = project.getDependencies().create(dep);
			if(shadedDep instanceof ModuleDependency) {
				((ModuleDependency) shadedDep).setTransitive(false);
			}

			project.getDependencies().add("shadow", dep);
		}
	}
}
