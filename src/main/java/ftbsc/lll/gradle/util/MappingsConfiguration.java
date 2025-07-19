package ftbsc.lll.gradle.util;

import ftbsc.lll.gradle.LilleroGradleExtension;
import ftbsc.lll.gradle.LilleroGradlePlugin;
import lombok.Getter;
import lombok.SneakyThrows;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.file.RegularFile;

import java.io.*;
import java.util.List;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Represents the configuration related to mappings.
 */
@Getter
public class MappingsConfiguration {
	private final Project project;
	private File mappings = null;
	private String namespaceFrom = null;
	private String namespaceTo = null;
	private boolean override = false;
	private Task taskIgnore = null;

	/**
	 * Creates a new {@link MappingsConfiguration} from the given extension.
	 * @param project the {@link Project} to create this for
	 * @param extension the {@link LilleroGradleExtension}
	 */
	public MappingsConfiguration(
		Project project,
		LilleroGradleExtension extension
	) {
		this.project = project;
		if(extension.getMappings().isPresent()) {
			RegularFile file = extension.getMappings().getOrNull();
			this.mappings = file != null ? file.getAsFile() : null;
			this.namespaceFrom = extension.getMappingsNamespaceFrom().getOrNull();
			this.namespaceTo =  extension.getMappingsNamespaceFrom().getOrNull();
			this.override = true;
		} else if(this.project.getPlugins().hasPlugin(LilleroGradlePlugin.LOOM_PLUGIN_ID)) {
			this.mappings = extract(project);
			this.namespaceFrom = "named";
			this.namespaceTo = "intermediary";
			this.taskIgnore = project.getTasks().findByName("runClient");;
		}
	}

	@SneakyThrows
	private static File extract(Project project) {
		Configuration mappingsConfig = project.getConfigurations().findByName("mappings");
		if(mappingsConfig != null) {
			File mappingsJar = mappingsConfig.getSingleFile();
			File extractedTiny = getMappingFile(project, mappingsConfig);
			try(ZipFile zip = new ZipFile(mappingsJar)) {
				ZipEntry tinyEntry = zip.getEntry("mappings/mappings.tiny");
				if(tinyEntry == null) {
					throw new RuntimeException("Bad mappings jar!");
				}

				long zipCrc = tinyEntry.getCrc();
				boolean needsExtract = true;

				if(extractedTiny.exists()) {
					long fileCrc = computeCRC32(extractedTiny);
					if(zipCrc == fileCrc) {
						needsExtract = false;
					}
				}

				if(needsExtract) {
					try(
						InputStream in = zip.getInputStream(tinyEntry);
						OutputStream out = new FileOutputStream(extractedTiny);
					) {
						byte[] buffer = new byte[8192];
						int len;
						while((len = in.read(buffer)) > 0) {
							out.write(buffer, 0, len);
						}
					}
				}
			}

			return extractedTiny;
		}

		throw new RuntimeException("Loom applied but no mappings found!");
	}

	private static File getMappingFile(Project project, Configuration mappingsConfig) {
		String versionSuffix = "unknown";
		Set<ResolvedArtifact> artifacts = mappingsConfig.getResolvedConfiguration().getResolvedArtifacts();
		for(ResolvedArtifact artifact : artifacts) {
			if(artifact.getName().equalsIgnoreCase("yarn")) {
				versionSuffix = artifact.getModuleVersion().getId().getVersion();
				break;
			}
		}

		File gradleDir = new File(project.getGradle().getGradleUserHomeDir(), "lillero/yarn");
		//noinspection ResultOfMethodCallIgnored
		gradleDir.mkdirs();

		return new File(gradleDir, "mappings-" + versionSuffix + ".tiny");
	}

	private static long computeCRC32(File file) throws IOException {
		CRC32 crc = new CRC32();
		try(InputStream in = new FileInputStream(file)) {
			byte[] buffer = new byte[8192];
			int len;
			while((len = in.read(buffer)) > 0) {
				crc.update(buffer, 0, len);
			}
		}

		return crc.getValue();
	}

	/**
	 * Appends the compiler arguments from this configuration to the given list.
	 * @param compilerArgs the list to append to
	 */
	public void appendCompilerArgs(List<String> compilerArgs) {
		if(this.taskIgnore != null && this.project.getGradle().getTaskGraph().hasTask(this.taskIgnore)) {
			return;
		}

		if(this.mappings != null) {
			compilerArgs.add("-AmappingsFile=" + this.mappings.getAbsolutePath());
			if(this.namespaceFrom != null && this.namespaceTo != null) {
				compilerArgs.add("-AmappingsNamespaceFrom=" + this.namespaceFrom);
				compilerArgs.add("-AmappingsNamespaceTo=" + this.namespaceTo);
			}
		}
	}
}
