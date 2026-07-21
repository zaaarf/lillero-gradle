package ftbsc.lll.gradle;

import ftbsc.lll.gradle.util.DependencyBlueprint;
import org.gradle.api.Project;
import org.gradle.api.Plugin;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.compile.JavaCompile;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

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

		Runnable configure = () -> configureDependencies(proj, extension);
		proj.getPluginManager().withPlugin(JAVA_PLUGIN_ID, p -> configure.run());
		proj.getPluginManager().withPlugin(JAVA_LIBRARY_PLUGIN_ID, p -> configure.run());
	}

	private static void configureDependencies(Project p, LilleroGradleExtension ext) {
		DependencyBlueprint core = new DependencyBlueprint(p, CORE_DEPSTRING, ext.getCoreVersion());
		p.getConfigurations().named("implementation").configure(c -> c.getDependencies().addLater(core.build()));
		shade(p, ext.getShadow(), core);

		DependencyBlueprint processor = new DependencyBlueprint(p, PROCESSOR_DEPSTRING, ext.getProcessorVersion());
		p.getConfigurations().named("compileOnly").configure(c -> c.getDependencies().addLater(processor.build()));
		p.getConfigurations().named("annotationProcessor").configure(c -> c.getDependencies().addLater(processor.build()));

		configureMixin(p, ext);
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

	private static void shade(Project project, Provider<Boolean> should, DependencyBlueprint blueprint) {
		Runnable doShade = () -> {
			Provider<Dependency> shaded = blueprint.build().map(dep -> {
				Dependency shadedDep = project.getDependencies().create(dep);

				if(shadedDep instanceof ModuleDependency) {
					((ModuleDependency) shadedDep).setTransitive(false);
				}

				return shadedDep;
			}).filter(d -> should.get());

			project.getConfigurations().named("shadow").configure(cfg -> cfg.getDependencies().addLater(shaded));
		};

		project.getPluginManager().withPlugin(SHADOW_PLUGIN_ID, p -> doShade.run());
		project.getPluginManager().withPlugin(SHADOW_OLD_PLUGIN_ID, p -> doShade.run());
	}

	// sometimes i think about making one of these for geb, then i remember what it feels like
	private static void configureMixin(Project project, LilleroGradleExtension ext) {
		Consumer<Boolean> doConfigure = supported -> {
			DependencyBlueprint mixin = new DependencyBlueprint(
				project,
				MIXIN_DEPSTRING,
				ext.getMixinVersion(),
				(p, d, v) -> v.isPresent() || ext.getAuto().isPresent() && supported
			);

			project.getConfigurations().named("implementation")
				.configure(c -> c.getDependencies().addLater(mixin.build()));

			shade(project, ext.getShadow(), mixin);
		};

		// loom and neoforged always support it, forge only in some versions
		project.getPluginManager().withPlugin(LOOM_PLUGIN_ID, p -> doConfigure.accept(true));
		project.getPluginManager().withPlugin(NEOFORGED_PLUGIN_ID, p -> doConfigure.accept(true));
		project.getPluginManager().withPlugin(FORGE_GRADLE_PLUGIN_ID, p -> {
			if(forgeVersionSupportsMixin(project)) {
				doConfigure.accept(true);
			}
		});

		doConfigure.accept(false);
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
