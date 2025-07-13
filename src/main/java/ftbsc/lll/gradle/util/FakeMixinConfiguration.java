package ftbsc.lll.gradle.util;

import ftbsc.lll.gradle.LilleroGradleExtension;

import java.util.List;

/**
 * Represents the configuration related to the fake mixin.
 */
public class FakeMixinConfiguration {
	private final String fakeMixin;
	private String outputPackage;

	/**
	 * Creates a new {@link FakeMixinConfiguration} from the given extension.
	 * @param extension the {@link LilleroGradleExtension}
	 */
	public FakeMixinConfiguration(LilleroGradleExtension extension) {
		this.fakeMixin = extension.getFakeMixinFQN().getOrNull();
		this.outputPackage = extension.getOutputPackage().getOrNull();
		if(this.outputPackage == null && this.fakeMixin != null) {
			this.outputPackage = this.fakeMixin.substring(0, this.fakeMixin.lastIndexOf('.'));
		}
	}

	/**
	 * Appends the compiler arguments from this configuration to the given list.
	 * @param compilerArgs the list to append to
	 */
	public void appendCompilerArgs(List<String> compilerArgs) {
		compilerArgs.add("-AfakeMixin=" + this.fakeMixin);
		compilerArgs.add("-AoutputPackage=" + this.outputPackage);
	}
}
