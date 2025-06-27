package io.github.forgestove.modaccessor;
import net.neoforged.accesstransformer.api.AccessTransformerEngine;
import org.gradle.api.artifacts.transform.*;
import org.gradle.api.file.*;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.*;
import org.jetbrains.annotations.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.ClassNode;

import javax.inject.Inject;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.jar.*;

import static java.util.stream.StreamSupport.stream;
@SuppressWarnings("deprecation")
public abstract class AccessTransform implements TransformAction<AccessTransform.Parameters> {
	/**
	 * {@link  org.gradle.api.internal.file.archive.ZipCopyAction#CONSTANT_TIME_FOR_ZIP_ENTRIES}
	 */
	private static final long CONSTANT_TIME_FOR_ZIP_ENTRIES = new GregorianCalendar(1980, Calendar.FEBRUARY, 1, 0, 0, 0).getTimeInMillis();
	@Contract(pure = true)
	@Inject
	public AccessTransform() {}
	@Override
	public void transform(@NotNull TransformOutputs outputs) {
		var artifact = getInputArtifact().get().getAsFile();
		if (!artifact.exists()) return;
		var parameters = getParameters();
		var atFiles = stream(parameters.getAccessTransformerFiles().spliterator(), false).map(File::toPath).toList();
		var engine = AccessTransformerEngine.newEngine();
		for (var atFile : atFiles) {
			try {
				engine.loadATFromPath(atFile);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		var outputFile = outputs.file(artifact.getName());
		try (var inputJar = new JarFile(artifact)) {
			try (var outputJarStream = new JarOutputStream(Files.newOutputStream(outputFile.toPath()))) {
				inputJar.stream().forEach(entry -> {
					try (var entryStream = inputJar.getInputStream(entry)) {
						if (entry.getName().endsWith(".class")) {
							var reader = new ClassReader(entryStream);
							var classNode = new ClassNode(Opcodes.ASM9);
							reader.accept(classNode, 0);
							final var type = Type.getType("L%s;".formatted(classNode.name.replaceAll("\\.", "/")));
							engine.transform(classNode, type);
							var classWriter = new ClassWriter(Opcodes.ASM5);
							classNode.accept(classWriter);
							var byteArray = classWriter.toByteArray();
							var newEntry = new JarEntry(entry.getName());
							newEntry.setTime(CONSTANT_TIME_FOR_ZIP_ENTRIES);
							outputJarStream.putNextEntry(newEntry);
							outputJarStream.write(byteArray);
							outputJarStream.closeEntry();
						} else {
							var newEntry = new JarEntry(entry.getName());
							newEntry.setTime(CONSTANT_TIME_FOR_ZIP_ENTRIES);
							outputJarStream.putNextEntry(newEntry);
							entryStream.transferTo(outputJarStream);
							outputJarStream.closeEntry();
						}
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				});
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	@InputArtifact
	@PathSensitive(PathSensitivity.NONE)
	public abstract Provider<FileSystemLocation> getInputArtifact();
	public interface Parameters extends TransformParameters {
		// Define any parameters you need for the transform
		@InputFiles
		@PathSensitive(PathSensitivity.NONE)
		ConfigurableFileCollection getAccessTransformerFiles();
	}
}
