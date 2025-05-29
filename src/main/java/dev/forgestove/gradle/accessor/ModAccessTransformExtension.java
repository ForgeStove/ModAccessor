package dev.forgestove.gradle.accessor;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Project;
import org.gradle.api.artifacts.*;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.file.ConfigurableFileCollection;

import javax.inject.Inject;
public abstract class ModAccessTransformExtension {
	static Attribute<Boolean> TRANSFORM_ACCESS = Attribute.of("accessor_plugin_transform_access", Boolean.class);
	private final Project project;
	@Inject
	public ModAccessTransformExtension(Project project) {
		this.project = project;
		// 自动收集 META-INF 下所有 .cfg 文件作为默认 accessTransformerFiles
		var metaInfDir = project.file("src/main/resources/META-INF");
		if (metaInfDir.exists() && metaInfDir.isDirectory()) {
			var cfgFiles = metaInfDir.listFiles((dir, name) -> name.endsWith(".cfg"));
			if (cfgFiles != null && cfgFiles.length > 0) getAccessTransformerFiles().from((Object[]) cfgFiles);
		}
	}
	public abstract ConfigurableFileCollection getAccessTransformerFiles();
	@SuppressWarnings({"UnusedReturnValue", "deprecation"})
	public Configuration createTransformConfiguration(Configuration parent) {
		var transformAccess = project.getConfigurations().create(
			"access" + StringUtils.capitalize(parent.getName()), spec -> {
				spec.setDescription("Configuration for dependencies of " + parent.getName() + " that needs to be remapped");
				spec.setCanBeConsumed(false);
				spec.setCanBeResolved(false);
				spec.setTransitive(false);
				//code based MDG legacy
				spec.withDependencies(dependencies -> dependencies.forEach(dep -> {
					if (dep instanceof ExternalModuleDependency emd) {
						project.getDependencies().constraints(constraints -> constraints.add(
							parent.getName(),
							"%s:%s:%s".formatted(emd.getGroup(), emd.getName(), emd.getVersion()),
							c -> c.attributes(a -> a.attribute(TRANSFORM_ACCESS, true))
						));
						emd.setTransitive(false);
					} else if (dep instanceof FileCollectionDependency fcd) {
						project.getDependencies().constraints(constraints -> constraints.add(
							parent.getName(),
							fcd.getFiles(),
							c -> c.attributes(a -> a.attribute(TRANSFORM_ACCESS, true))
						));
					} else if (dep instanceof ProjectDependency pd) {
						project.getDependencies().constraints(constraints -> constraints.add(
							parent.getName(),
							pd.getDependencyProject(),
							c -> c.attributes(a -> a.attribute(TRANSFORM_ACCESS, true))
						));
						pd.setTransitive(false);
					}
				}));
			}
		);
		parent.extendsFrom(transformAccess);
		transformAccess.getAttributes().attribute(TRANSFORM_ACCESS, false);
		return transformAccess;
	}
}
