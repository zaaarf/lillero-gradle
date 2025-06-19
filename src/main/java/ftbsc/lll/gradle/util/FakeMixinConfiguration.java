package ftbsc.lll.gradle.util;

import ftbsc.lll.gradle.LilleroGradleExtension;

import java.util.List;

public class FakeMixinConfiguration {
	private final String fakeMixin;
	private String outputPackage;

	public FakeMixinConfiguration(LilleroGradleExtension extension) {
		this.fakeMixin = extension.getFakeMixinFQN().getOrNull();
		this.outputPackage = extension.getOutputPackage().getOrNull();
		if(this.outputPackage == null && this.fakeMixin != null) {
			this.outputPackage = this.fakeMixin.substring(0, this.fakeMixin.lastIndexOf('.'));
		}
	}

	public void appendCompilerArgs(List<String> compilerArgs) {
		compilerArgs.add("-AfakeMixin=" + this.fakeMixin);
		compilerArgs.add("-AoutputPackage=" + this.outputPackage);
	}
}
