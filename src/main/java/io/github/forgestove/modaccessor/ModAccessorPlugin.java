package io.github.forgestove.modaccessor;
import org.gradle.api.*;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.gradle.api.attributes.AttributeContainer;
import org.jetbrains.annotations.NotNull;

import java.io.File;
@SuppressWarnings("unused")
public class ModAccessorPlugin implements Plugin<@NotNull Project> {
	@Override
	public void apply(@NotNull Project project) {
		var extension = project.getExtensions().create("modAccessor", ModAccessorExtension.class, project);
		project.getDependencies()
			.getArtifactTypes()
			.named(ArtifactTypeDefinition.JAR_TYPE, type -> type.getAttributes().attribute(ModAccessorExtension.TRANSFORM_ACCESS, false));
		project.getDependencies().registerTransform(
			AccessTransform.class, parameters -> {
				parameters.parameters(p -> p.getAccessTransformerFiles().from(extension.getAccessTransformerFiles()));
				getAttribute(parameters.getFrom(), false, false);
				getAttribute(parameters.getTo(), true, false);
			}
		);
		project.getDependencies().registerTransform(
			InterfaceInjectionTransform.class, parameters -> {
				parameters.parameters(p -> p.getInterfaceInjectionFiles().from(extension.getInterfaceInjectionFiles()));
				getAttribute(parameters.getFrom(), true, false);
				getAttribute(parameters.getTo(), true, true);
			}
		);
		for (var config : project.getConfigurations()) extension.createTransformConfiguration(config);
		project.afterEvaluate(p -> {
			var atFiles = extension.getAccessTransformerFiles();
			if (atFiles.isEmpty() || atFiles.getFiles().stream().noneMatch(File::exists))
				p.getLogger().error("[ModAccessor]: No access transformer files found. Please add some.");
		});
	}
	private void getAttribute(AttributeContainer parameters, boolean v1, boolean v2) {
		parameters.attribute(ModAccessorExtension.TRANSFORM_ACCESS, v1)
			.attribute(ModAccessorExtension.TRANSFORM_INTERFACE_INJECT, v2)
			.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.JAR_TYPE);
	}
}
