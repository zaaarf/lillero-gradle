package ftbsc.lll.gradle;

import ftbsc.lll.gradle.util.FakeMixinConfiguration;
import ftbsc.lll.gradle.util.MappingsConfiguration;
import lombok.Getter;
import org.gradle.api.Project;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

import javax.inject.Inject;

/**
 * lillero-gradle's extension point, allowing user customisation.
 */
public abstract class LilleroGradleExtension {
	/**
	 * The project this is for.
	 */
	private final Project project;

	/**
	 * Whether this should register the custom repository implicitly.
	 * Defaults to true.
	 */
	@Getter
	private final Property<Boolean> registerRepo;

	/**
	 * Whether this should attempt to pick the loader most suited to your
	 * environment. Currently only supports loom.
	 * Defaults to true.
	 */
	@Getter
	private final Property<Boolean> auto;

	/**
	 * The version of lillero to use.
	 * Defaults to the latest.
	 */
	@Getter
	private final Property<String> coreVersion;

	/**
	 * The version of lillero-processor to use.
	 * Defaults to the latest.
	 */
	@Getter
	private final Property<String> processorVersion;

	/**
	 * The version of lillero-mixin to use.
	 * Defaults to the latest.
	 */
	@Getter
	private final Property<String> mixinVersion;

	/**
	 * The version of lillero-loader to use.
	 * Defaults to the latest.
	 */
	@Getter
	private final Property<String> loaderVersion;

	/**
	 * Whether this should attempt to shadow lillero in the plugin.
	 * Defaults to true; will be a no-op if the shadow plugin is not present.
	 */
	@Getter
	private final Property<Boolean> shadow;

	/**
	 * The fully-qualified name to the fake mixin.
	 * You still need to specify it in your mod's configuration.
	 * Has no default value.
	 */
	@Getter
	private final Property<String> fakeMixinFQN;

	/**
	 * The output package for the classes.
	 * Defaults to the package of the fake mixin (or to unset, if no fake mixin is provided).
	 */
	@Getter
	private final Property<String> outputPackage;


	/**
	 * Custom mappings to use instead of autoloaded ones.
	 * Defaults to the latest.
	 */
	@Getter
	private final RegularFileProperty mappings;

	/**
	 * The namespace to map from.
	 * Has no default value.
	 */
	@Getter
	private final Property<String> mappingsNamespaceFrom;

	/**
	 * The namespace to map to.
	 * Has no default value.
	 */
	@Getter
	private final Property<String> mappingsNamespaceTo;

	/**
	 * Whether to warn about unverifiable anonymous classes.
	 * Defaults to true.
	 */
	@Getter
	private final Property<Boolean> anonymousClassWarning;

	/**
	 * Whether to warn about manually specified fully qualified names that can't be validated.
	 * Defaults to true.
	 */
	@Getter
	private final Property<Boolean> manualClassWarning;

	/**
	 * Whether generated IInjectors should use obfuscated names at runtime
	 * Defaults to false.
	 */
	@Getter
	private final Property<Boolean> obfuscateInjectorMetadata;

	/**
	 * Whether this should avoid generating service a provider file.
	 * Defaults to false.
	 */
	@Getter
	private final Property<Boolean> noServiceProvider;

	@Inject
	public LilleroGradleExtension(ObjectFactory factory, Project project) {
		this.project = project;
		this.registerRepo = factory.property(Boolean.class).convention(true);
		this.auto = factory.property(Boolean.class).convention(true);
		this.coreVersion = factory.property(String.class).convention("+");
		this.mixinVersion = factory.property(String.class).convention("+");
		this.loaderVersion = factory.property(String.class).convention("+");
		this.processorVersion = factory.property(String.class).convention("+");
		this.shadow = factory.property(Boolean.class).convention(true);
		this.fakeMixinFQN = factory.property(String.class);
		this.outputPackage = factory.property(String.class);
		this.mappings = factory.fileProperty();
		this.mappingsNamespaceFrom = factory.property(String.class);
		this.mappingsNamespaceTo = factory.property(String.class);
		this.anonymousClassWarning = factory.property(Boolean.class).convention(true);
		this.manualClassWarning = factory.property(Boolean.class).convention(true);
		this.obfuscateInjectorMetadata = factory.property(Boolean.class).convention(false);
		this.noServiceProvider = factory.property(Boolean.class).convention(false);
	}

	private MappingsConfiguration mapCfg = null;
	public MappingsConfiguration getMappingsConfiguration() {
		return this.mapCfg != null ? this.mapCfg : (this.mapCfg = new MappingsConfiguration(this.project, this));
	}

	private FakeMixinConfiguration fmCfg = null;
	public FakeMixinConfiguration getFakeMixinConfiguration() {
		return this.fmCfg != null ? this.fmCfg : (this.fmCfg = new FakeMixinConfiguration(this));
	}
}
