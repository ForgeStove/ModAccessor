package io.github.forgestove.modaccessor;
import com.google.gson.JsonParser;
import io.github.forgestove.modaccessor.InterfaceInjectionTransform.Parameters;
import org.gradle.api.artifacts.transform.*;
import org.gradle.api.file.*;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.*;
import org.jetbrains.annotations.*;
import org.objectweb.asm.*;
import org.objectweb.asm.signature.*;
import org.objectweb.asm.util.CheckSignatureAdapter;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.jar.*;

import static java.util.stream.StreamSupport.stream;
public abstract class InterfaceInjectionTransform implements TransformAction<@NotNull Parameters> {
	private static final long CONSTANT_TIME_FOR_ZIP_ENTRIES = new GregorianCalendar(1980, Calendar.FEBRUARY, 1, 0, 0, 0).getTimeInMillis();
	@InputArtifact
	@PathSensitive(PathSensitivity.NONE)
	public abstract Provider<@NotNull FileSystemLocation> getInputArtifact();
	@Override
	public void transform(@NotNull TransformOutputs outputs) {
		try {
			var artifact = getInputArtifact().get().getAsFile();
			if (!artifact.exists()) return;
			var parameters = getParameters();
			var injectionFilePaths = stream(parameters.getInterfaceInjectionFiles().spliterator(), false).map(File::toPath).toList();
			Map<String, List<String>> injections = new HashMap<>();
			for (var injectionFilePath : injectionFilePaths)
				try {
					var jsonObject = JsonParser.parseReader(Files.newBufferedReader(injectionFilePath)).getAsJsonObject();
					for (var entry : jsonObject.entrySet()) {
						List<String> list = new ArrayList<>();
						if (entry.getValue().isJsonArray()) {
							var array = entry.getValue().getAsJsonArray();
							for (var element : array) list.add(element.getAsString());
						}
						if (entry.getValue().isJsonPrimitive()) list.add(entry.getValue().getAsString());
						injections.put(entry.getKey(), list);
					}
				} catch (IOException e) {
					throw new RuntimeException("Failed to parse injection file: " + injectionFilePath, e);
				}
			var outputFile = outputs.file("injected-" + artifact.getName());
			if (injections.isEmpty()) {
				try {
					Files.copy(artifact.toPath(), outputFile.toPath());
				} catch (IOException e) {throw new RuntimeException(e);}
				return;
			}
			try (var inputJar = new JarFile(artifact)) {
				try (var outputJarStream = new JarOutputStream(Files.newOutputStream(outputFile.toPath()))) {
					inputJar.stream().forEach(entry -> {
						try (var entryStream = inputJar.getInputStream(entry)) {
							if (entry.getName().endsWith(".class")) {
								var reader = new ClassReader(entryStream);
								var classType = entry.getName().replace(".class", "");
								var toInject = injections.get(classType);
								var classWriter = new ClassWriter(Opcodes.ASM9);
								if (toInject != null) {
									var toApply = toInject.stream().map(toImpl -> InterfaceInjection.of(classType, toImpl)).toList();
									reader.accept(new InjectVisitor(Opcodes.ASM9, classWriter, toApply), 0);
								} else reader.accept(classWriter, 0);
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
		} catch (RuntimeException e) {
			throw new RuntimeException(e);
		}
		System.out.println();
	}
	public interface Parameters extends TransformParameters {
		// Define any parameters you need for the transform
		@InputFiles
		@PathSensitive(PathSensitivity.NONE)
		ConfigurableFileCollection getInterfaceInjectionFiles();
	}
	//Code From:https://github.com/FabricMC/fabric-loom/blob/7c53939918cf63cdf4f176847088fd747c61e993/src/main/java/net/fabricmc/loom/configuration/ifaceinject/InterfaceInjectionProcessor.java
	private record InterfaceInjection(String target, String toImpl, @Nullable String generics) {
		public static InterfaceInjection of(String target, String toImpl) {
			var type = toImpl;
			String generics = null;
			if (toImpl.contains("<") && toImpl.contains(">")) {
				var start = toImpl.indexOf('<');
				var end = toImpl.lastIndexOf('>');
				type = toImpl.substring(0, start);
				// Extract the generics part and replace '.' with '/'
				var processedGenerics = getStringBuilder(toImpl, start, end);
				generics = processedGenerics.toString();
				// First Generics Check, if there are generics, are they correctly written?
				var reader = new SignatureReader("Ljava/lang/Object" + generics + ";");
				// Assuming CheckSignatureAdapter is a class that can handle the signature and reader is defined somewhere above
				var checker = new CheckSignatureAdapter(CheckSignatureAdapter.CLASS_SIGNATURE, null);
				reader.accept(checker);
			}
			return new InterfaceInjection(target, type, generics);
		}
		private static String processNestedGenerics(String component) {
			var start = component.indexOf('<');
			var end = component.lastIndexOf('>');
			var outerType = component.substring(0, start);
			var innerProcessedGenerics = getStringBuilder(component, start, end);
			return "L" + outerType + innerProcessedGenerics;
		}
		private static @NotNull StringBuilder getStringBuilder(String component, int start, int end) {
			var innerRawGenerics = component.substring(start + 1, end).replace('.', '/');
			// Split the inner generics into individual components
			var innerGenericComponents = innerRawGenerics.split(",");
			var innerProcessedGenerics = new StringBuilder("<");
			for (var i = 0; i < innerGenericComponents.length; i++) {
				var innerComponent = innerGenericComponents[i].trim();
				// Handle nested generics recursively
				// Handle simple types
				if (innerComponent.contains("<")) innerComponent = processNestedGenerics(innerComponent);
				else innerComponent = "L" + innerComponent + ";";
				innerProcessedGenerics.append(innerComponent);
				if (i < innerGenericComponents.length - 1) innerProcessedGenerics.append(",");
			}
			innerProcessedGenerics.append(">");
			return innerProcessedGenerics;
		}
	}
	private static class InjectVisitor extends ClassVisitor {
		private static final int INTERFACE_ACCESS = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE;
		private final List<InterfaceInjection> interfaceInjections;
		private final Set<String> knownInnerClasses = new HashSet<>();
		InjectVisitor(int asmVersion, ClassWriter writer, List<InterfaceInjection> interfaceInjections) {
			super(asmVersion, writer);
			this.interfaceInjections = interfaceInjections;
		}
		@Override
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			var baseInterfaces = interfaces.clone();
			Set<String> modifiedInterfaces = new LinkedHashSet<>(interfaces.length + interfaceInjections.size());
			Collections.addAll(modifiedInterfaces, interfaces);
			for (var interfaceInjection : interfaceInjections) modifiedInterfaces.add(interfaceInjection.toImpl());
			// See JVMS: https://docs.oracle.com/javase/specs/jvms/se17/html/jvms-4.html#jvms-ClassSignature
			if (interfaceInjections.stream().anyMatch(injection -> injection.generics != null) && signature == null) {
				// Classes that are not using generics don't need signatures, so their signatures are null
				// If the class is not using generics but that an injected interface targeting the class is using them, we are creating
				// the class signature
				var baseSignatureBuilder = new StringBuilder("L" + superName + ";");
				for (var baseInterface : baseInterfaces) baseSignatureBuilder.append("L").append(baseInterface).append(";");
				signature = baseSignatureBuilder.toString();
			}
			if (signature != null) {
				var reader = new SignatureReader(signature);
				// Second Generics Check, if there are passed generics, are all of them present in the target class?
				var checker = new GenericsChecker(api, interfaceInjections);
				reader.accept(checker);
				checker.check();
				var resultingSignature = new StringBuilder(signature);
				for (var interfaceInjection : interfaceInjections) {
					String superinterfaceSignature;
					if (interfaceInjection.generics() != null)
						superinterfaceSignature = "L" + interfaceInjection.toImpl() + interfaceInjection.generics() + ";";
					else superinterfaceSignature = "L" + interfaceInjection.toImpl() + ";";
					if (resultingSignature.indexOf(superinterfaceSignature) == -1) resultingSignature.append(superinterfaceSignature);
				}
				signature = resultingSignature.toString();
			}
			super.visit(version, access, name, signature, superName, modifiedInterfaces.toArray(new String[0]));
		}
		@Override
		public void visitInnerClass(final String name, final String outerName, final String innerName, final int access) {
			this.knownInnerClasses.add(name);
			super.visitInnerClass(name, outerName, innerName, access);
		}
		@Override
		public void visitEnd() {
			// inject any necessary inner class entries
			// this may produce technically incorrect bytecode cuz we don't know the actual access flags for inner class entries,
			// but it's hopefully enough to quiet some IDE errors
			for (final var itf : interfaceInjections) {
				if (this.knownInnerClasses.contains(itf.toImpl())) continue;
				var simpleNameIdx = itf.toImpl().lastIndexOf('/');
				final var simpleName = simpleNameIdx == -1 ? itf.toImpl() : itf.toImpl().substring(simpleNameIdx + 1);
				var lastIdx = -1;
				var dollarIdx = -1;
				// Iterate through inner class entries starting from outermost to innermost
				while ((dollarIdx = simpleName.indexOf('$', dollarIdx + 1)) != -1) {
					if (dollarIdx - lastIdx == 1) continue;
					// Emit the inner class entry from this to the last one
					if (lastIdx != -1) {
						final var outerName = itf.toImpl().substring(0, simpleNameIdx + 1 + lastIdx);
						final var innerName = simpleName.substring(lastIdx + 1, dollarIdx);
						super.visitInnerClass(outerName + '$' + innerName, outerName, innerName, INTERFACE_ACCESS);
					}
					lastIdx = dollarIdx;
				}
				// If we have a trailer to append
				if (lastIdx != -1 && lastIdx != simpleName.length()) {
					final var outerName = itf.toImpl().substring(0, simpleNameIdx + 1 + lastIdx);
					final var innerName = simpleName.substring(lastIdx + 1);
					super.visitInnerClass(outerName + '$' + innerName, outerName, innerName, INTERFACE_ACCESS);
				}
			}
			super.visitEnd();
		}
	}
	private static class GenericsChecker extends SignatureVisitor {
		private final List<String> typeParameters;
		private final List<InterfaceInjection> interfaceInjections;
		GenericsChecker(int asmVersion, List<InterfaceInjection> interfaceInjections) {
			super(asmVersion);
			this.typeParameters = new ArrayList<>();
			this.interfaceInjections = interfaceInjections;
		}
		@Override
		public void visitFormalTypeParameter(String name) {
			this.typeParameters.add(name);
			super.visitFormalTypeParameter(name);
		}
		// Ensures that injected interfaces only use collected type parameters from the target class
		public void check() {
			for (var interfaceInjection : this.interfaceInjections)
				if (interfaceInjection.generics() != null) {
					var reader = new SignatureReader("Ljava/lang/Object" + interfaceInjection.generics() + ";");
					var confirm = new GenericsConfirm(api, interfaceInjection.target(), interfaceInjection.toImpl(), this.typeParameters);
					reader.accept(confirm);
				}
		}
	}
	private static class GenericsConfirm extends SignatureVisitor {
		private final String className;
		private final String interfaceName;
		private final List<String> acceptedTypeVariables;
		GenericsConfirm(int asmVersion, String className, String interfaceName, List<String> acceptedTypeVariables) {
			super(asmVersion);
			this.className = className;
			this.interfaceName = interfaceName;
			this.acceptedTypeVariables = acceptedTypeVariables;
		}
		@Override
		public void visitTypeVariable(String name) {
			if (!this.acceptedTypeVariables.contains(name)) throw new IllegalStateException("Interface "
				+ this.interfaceName
				+ " attempted to use a type variable named "
				+ name
				+ " which is not present in the "
				+ this.className
				+ " class");
			super.visitTypeVariable(name);
		}
	}
}
