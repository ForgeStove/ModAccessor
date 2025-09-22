package io.github.forgestove.modaccessor;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Project;
import org.gradle.api.artifacts.*;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.file.ConfigurableFileCollection;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
public abstract class ModAccessorExtension {
	static Attribute<@NotNull Boolean> TRANSFORM_ACCESS = Attribute.of("accessor_plugin_transform_access", Boolean.class);
	static Attribute<@NotNull Boolean> TRANSFORM_INTERFACE_INJECT = Attribute.of(
		"accessor_plugin_transform_interface_inject",
		Boolean.class
	);
	private final Project project;
	@Inject
	public ModAccessorExtension(@NotNull Project project) {
		this.project = project;
		// Automatically collect all *.cfg files under META-INF as the default accessTransformerFiles
		var metaInfDir = project.file("src/main/resources/META-INF");
		if (!metaInfDir.exists() || !metaInfDir.isDirectory()) return;
		var cfgFiles = metaInfDir.listFiles((dir, name) -> name.endsWith(".cfg"));
		if (cfgFiles != null && cfgFiles.length > 0) getAccessTransformerFiles().from((Object[]) cfgFiles);
	}
	public abstract ConfigurableFileCollection getInterfaceInjectionFiles();
	public abstract ConfigurableFileCollection getAccessTransformerFiles();
	@SuppressWarnings("UnusedReturnValue")
	public Configuration createTransformConfiguration(Configuration parent) {
		var transformJar = project.getConfigurations().create(
			"access" + StringUtils.capitalize(parent.getName()), spec -> {
				spec.setDescription("Configuration for dependencies of " + parent.getName() + " that needs to be remapped");
				spec.setCanBeConsumed(false);
				spec.setCanBeResolved(false);
				spec.setTransitive(false);
				//code based MDG legacy
				spec.withDependencies(dependencies -> dependencies.forEach(dep -> {
					if (dep instanceof ExternalModuleDependency emd) {
						project.getDependencies().constraints(constraints -> constraints.add(
							parent.getName(), "%s:%s:%s".formatted(emd.getGroup(), emd.getName(), emd.getVersion()), c -> {
								c.attributes(a -> a.attribute(TRANSFORM_ACCESS, true));
								c.attributes(a -> a.attribute(TRANSFORM_INTERFACE_INJECT, true));
							}
						));
						emd.setTransitive(false);
					} else if (dep instanceof FileCollectionDependency fcd)
						project.getDependencies().constraints(constraints -> constraints.add(
							parent.getName(), fcd.getFiles(), c -> {
								c.attributes(a -> a.attribute(TRANSFORM_ACCESS, true));
								c.attributes(a -> a.attribute(TRANSFORM_INTERFACE_INJECT, true));
							}
						));
					else if (dep instanceof ProjectDependency pd) {
						pd.attributes(a -> {
							a.attribute(TRANSFORM_ACCESS, true);
							a.attribute(TRANSFORM_INTERFACE_INJECT, true);
						});
						pd.setTransitive(false);
					}
				}));
			}
		);
		parent.extendsFrom(transformJar);
		transformJar.getAttributes().attribute(TRANSFORM_ACCESS, false);
		transformJar.getAttributes().attribute(TRANSFORM_INTERFACE_INJECT, false);
		return transformJar;
	}
}
