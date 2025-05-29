package dev.forgestove.gradle.accessor;
import org.gradle.api.*;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.jetbrains.annotations.NotNull;

import java.io.File;
@SuppressWarnings("unused")
public class ModAccessorPlugin implements Plugin<Project> {
	@Override
	public void apply(@NotNull Project project) {
		var extension = project.getExtensions().create("modAccessor", ModAccessTransformExtension.class, project);
		project.getDependencies().getArtifactTypes().named(
			ArtifactTypeDefinition.JAR_TYPE,
			type -> type.getAttributes().attribute(ModAccessTransformExtension.TRANSFORM_ACCESS, false)
		);
		project.getDependencies().registerTransform(
			AccessTransform.class, parameters -> {
				parameters.parameters(p -> p.getAccessTransformerFiles().from(extension.getAccessTransformerFiles()));
				parameters.getFrom().attribute(ModAccessTransformExtension.TRANSFORM_ACCESS, false)
						  .attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.JAR_TYPE);
				parameters.getTo().attribute(ModAccessTransformExtension.TRANSFORM_ACCESS, true)
						  .attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.JAR_TYPE);
			}
		);
		var baseConfigs = new String[]{"compileOnly", "implementation", "api"};
		for (var base : baseConfigs) {
			var config = project.getConfigurations().findByName(base);
			if (config != null) extension.createTransformConfiguration(config);
		}
		project.afterEvaluate(p -> {
			var atFiles = extension.getAccessTransformerFiles();
			if (atFiles.isEmpty() || atFiles.getFiles().stream().noneMatch(File::exists)) {
				p.getLogger().error("No access transformer files found. Please add some.");
			}
		});
	}
}
