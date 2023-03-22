package com.regnosys.rosetta.generator.java;

import com.google.common.collect.Streams;
import com.regnosys.rosetta.generator.GeneratedIdentifier;
import com.regnosys.rosetta.generator.GeneratorScope;
import com.regnosys.rosetta.generator.ImplicitVariableRepresentation;
import com.regnosys.rosetta.generator.java.types.JavaClass;
import com.regnosys.rosetta.generator.java.types.JavaType;
import com.regnosys.rosetta.utils.DottedPath;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.lang.model.SourceVersion;

public class JavaScope extends GeneratorScope<JavaScope> {
	private final Set<DottedPath> defaultPackages = new HashSet<>();
	private final Set<BlueprintImplicitVariableRepresentation> blueprintVars = new HashSet<>();
	
	public JavaScope(DottedPath packageName) {
		super("Top[" + packageName.withDots() + "]");
		this.defaultPackages.add(DottedPath.of("java", "lang"));
		this.defaultPackages.add(packageName);
	}
	protected JavaScope(String description, JavaScope parent) {
		super(description, parent);
	}

	@Override
	public JavaScope childScope(String description) {
		return new JavaScope(description, this);
	}
	public JavaScope classScope(String className) {
		return childScope("Class[" + className + "]");
	}
	public JavaScope methodScope(String methodName) {
		return childScope("Method[" + methodName + "]");
	}
	public JavaScope lambdaScope() {
		return childScope("Lambda[]");
	}

	@Override
	public boolean isValidIdentifier(String name) {
		return SourceVersion.isName(name);
	}
	
	// Make sure identifiers from package "java.lang" is always in scope.
	@Override
	public Optional<GeneratedIdentifier> getIdentifier(Object obj) {
		return super.getIdentifier(obj).or(() -> {
			JavaType t = JavaType.from(obj);
			if (t != null) {
				if (t instanceof JavaClass) {
					JavaClass clazz = (JavaClass)t;
					if (this.defaultPackages.contains(clazz.getPackageName())) {
						return Optional.of(new DefaultScopeIdentifier(this, clazz.getCanonicalName()));
					}
				}
			}
			if (obj instanceof BlueprintImplicitVariableRepresentation) {
				BlueprintImplicitVariableRepresentation repr = (BlueprintImplicitVariableRepresentation)obj;
				return this.getAllBlueprintVars()
						.filter(otherRepr -> repr.match(otherRepr))
						.findFirst()
						.flatMap(otherRepr -> getIdentifier(otherRepr))
						.or(() -> getIdentifier(new ImplicitVariableRepresentation(repr.getType().getData())));
			}
			return Optional.empty();
		});
	}
	
	@Override
	public GeneratedIdentifier createIdentifier(Object obj, String name) {
		GeneratedIdentifier id = super.createIdentifier(obj, name);
		if (obj instanceof BlueprintImplicitVariableRepresentation) {
			this.blueprintVars.add((BlueprintImplicitVariableRepresentation)obj);
		}
		return id;
	}
	
	private Stream<BlueprintImplicitVariableRepresentation> getAllBlueprintVars() {
		return Streams.concat(
				this.getParent().map(p -> p.getAllBlueprintVars()).orElseGet(() -> Stream.empty()),
				this.blueprintVars.stream()
			);
	}
	
	private static class DefaultScopeIdentifier extends GeneratedIdentifier {
		private final DottedPath canonicalName;
		
		public DefaultScopeIdentifier(GeneratorScope<?> scope, DottedPath canonicalName) {
			super(scope, canonicalName.last());
			this.canonicalName = canonicalName;
		}
		
		@Override
		protected String getActualName() {
			if (this.scope.getIdentifiers().stream().anyMatch(id -> id.getDesiredName().equals(this.getDesiredName()))) {
				return this.canonicalName.withDots();
			}
			return this.getDesiredName();
		}
	}
}