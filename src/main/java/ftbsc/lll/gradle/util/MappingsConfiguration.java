package ftbsc.lll.gradle.util;

import ftbsc.lll.gradle.LilleroGradleExtension;
import ftbsc.lll.gradle.LilleroGradlePlugin;
import lombok.Getter;
import org.gradle.api.Project;
import org.gradle.api.file.RegularFile;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

/**
 * Object representing a project's mapping configuration.
 */
@Getter
public class MappingsConfiguration {
	private final Project project;
	private Path mappings = null;
	private String namespaceFrom = null;
	private String namespaceTo = null;
	private boolean override = false;
	private String taskIgnore = null;

	public MappingsConfiguration(
		Project project,
		LilleroGradleExtension extension
	) {
		this.project = project;
		if(extension.getMappings().isPresent()) {
			RegularFile file = extension.getMappings().getOrNull();
			this.mappings = file != null ? file.getAsFile().toPath() : null;
			this.namespaceFrom = extension.getMappingsNamespaceFrom().getOrNull();
			this.namespaceTo =  extension.getMappingsNamespaceFrom().getOrNull();
			this.override = true;
		} else if(this.project.getPlugins().hasPlugin(LilleroGradlePlugin.LOOM_PLUGIN_ID)) {
			this.mappings = project.getLayout().getBuildDirectory().get()
				.getAsFile()
				.toPath()
				.getParent()
				.resolve(".gradle/lillero/mappings.tiny"); // TODO there HAS to be a better way
			this.namespaceFrom = "named";
			this.namespaceTo = "intermediary";
			this.taskIgnore = "runClient";
		}
	}

	public void appendCompilerArgs(List<String> compilerArgs) {
		if(this.taskIgnore != null && this.project.getGradle().getTaskGraph().hasTask(this.taskIgnore)) {
			return;
		}

		if(
			this.mappings != null
				&& (this.taskIgnore == null || this.project.getGradle().getTaskGraph().hasTask(this.taskIgnore))
		) {
			File mappingFile = this.mappings.toFile();
			compilerArgs.add("-AmappingsFile=" + mappingFile.getAbsolutePath());
			if(this.namespaceFrom != null && this.namespaceTo != null) {
				compilerArgs.add("-AmappingsNamespaceFrom=" + this.namespaceFrom);
				compilerArgs.add("-AmappingsNamespaceTo=" + this.namespaceTo);
			}
		}
	}
}
