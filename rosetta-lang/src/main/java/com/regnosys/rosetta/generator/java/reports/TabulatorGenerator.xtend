package com.regnosys.rosetta.generator.java.reports

import com.google.inject.ImplementedBy
import com.regnosys.rosetta.RosettaEcoreUtil
import com.regnosys.rosetta.config.RosettaConfiguration
import com.regnosys.rosetta.generator.GeneratedIdentifier
import com.regnosys.rosetta.generator.java.JavaScope
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator
import com.regnosys.rosetta.generator.java.util.ImportManagerExtension
import com.regnosys.rosetta.rosetta.RosettaExternalRuleSource
import com.regnosys.rosetta.rosetta.RosettaReport
import com.regnosys.rosetta.rosetta.RosettaRule
import com.regnosys.rosetta.rosetta.simple.Data
import com.regnosys.rosetta.rosetta.simple.Function
import com.regnosys.rosetta.types.RAttribute
import com.regnosys.rosetta.types.RDataType
import com.regnosys.rosetta.types.RObjectFactory
import com.regnosys.rosetta.types.RosettaTypeProvider
import com.regnosys.rosetta.utils.ExternalAnnotationUtil
import com.regnosys.rosetta.utils.ModelIdProvider
import com.rosetta.model.lib.ModelSymbolId
import com.rosetta.model.lib.reports.Tabulator
import com.rosetta.model.lib.reports.Tabulator.Field
import com.rosetta.model.lib.reports.Tabulator.FieldImpl
import com.rosetta.model.lib.reports.Tabulator.FieldValue
import com.rosetta.model.lib.reports.Tabulator.FieldValueImpl
import com.rosetta.model.lib.reports.Tabulator.MultiNestedFieldValueImpl
import com.rosetta.model.lib.reports.Tabulator.NestedFieldValueImpl
import com.rosetta.util.DottedPath
import com.rosetta.util.types.JavaClass
import java.util.Arrays
import java.util.List
import java.util.Map
import java.util.Objects
import java.util.Optional
import java.util.Set
import java.util.stream.Collectors
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.apache.commons.text.StringEscapeUtils
import org.eclipse.xtend2.lib.StringConcatenationClient
import org.eclipse.xtext.generator.IFileSystemAccess2
import com.regnosys.rosetta.types.RChoiceType
import com.regnosys.rosetta.generator.java.types.JavaPojoInterface

class TabulatorGenerator {
	private interface TabulatorContext {
		def boolean needsTabulator(RDataType type)
		def boolean isTabulated(RAttribute attr)
		def JavaClass<Tabulator<?>> toTabulatorJavaClass(RDataType type)
		def Optional<RosettaRule> getRule(RAttribute attr)
		def Function getFunction()
	}

	@org.eclipse.xtend.lib.annotations.Data
	private static class ReportTabulatorContext implements TabulatorContext {
		extension RosettaEcoreUtil
		extension JavaTypeTranslator
		extension RosettaTypeProvider
		Map<RAttribute, RosettaRule> ruleMap
		Optional<RosettaExternalRuleSource> ruleSource
		
		override needsTabulator(RDataType type) {
			needsTabulator(type, newHashSet)
		}
		private def boolean needsTabulator(RDataType type, Set<Data> visited) {
			if (visited.add(type.EObject)) {
				type.allAttributes.exists[isTabulated(visited)]
			} else {
				false
			}
		}
		override boolean isTabulated(RAttribute attr) {
			isTabulated(attr, newHashSet)
		}
		private def boolean isTabulated(RAttribute attr, Set<Data> visited) {
			val rawAttrType = attr.RMetaAnnotatedType.RType
			val attrType = if (rawAttrType instanceof RChoiceType) {
				rawAttrType.asRDataType
			} else {
				rawAttrType
			}
			if (attrType instanceof RDataType && needsTabulator(attrType as RDataType, visited)) {
				true
			} else {
				ruleMap.containsKey(attr)
			}
		}
		
		override toTabulatorJavaClass(RDataType type) {
			type.EObject.toTabulatorJavaClass(ruleSource)
		}
		
		override getRule(RAttribute attr) {
			Optional.ofNullable(ruleMap.get(attr))
		}
		
		override getFunction() {
			throw new UnsupportedOperationException("getFunction not available for ReportTabulatorContext")
		}
		
	}
	@Deprecated
	@org.eclipse.xtend.lib.annotations.Data
	private static class ProjectionTabulatorContext implements TabulatorContext {
		extension JavaTypeTranslator
		Function projection
		
		override needsTabulator(RDataType type) {
			true
		}
		
		override isTabulated(RAttribute attr) {
			true
		}
		
		override toTabulatorJavaClass(RDataType type) {
			type.EObject.toProjectionTabulatorJavaClass(projection)
		}
		
		override getRule(RAttribute attr) {
			Optional.empty
		}
		
		override getFunction() {
			projection
		}
		

	}
	@org.eclipse.xtend.lib.annotations.Data
	private static class FunctionTabulatorContext implements TabulatorContext {
		extension JavaTypeTranslator
		Function function
		
		override needsTabulator(RDataType type) {
			true
		}
		
		override isTabulated(RAttribute attr) {
			true
		}
		
		override toTabulatorJavaClass(RDataType type) {
			type.EObject.toTabulatorJavaClass(function)
		}
		
		override getRule(RAttribute attr) {
			Optional.empty
		}

	}
	@org.eclipse.xtend.lib.annotations.Data
	private static class DataTabulatorContext implements TabulatorContext {
		extension JavaTypeTranslator typeTranslator

		override needsTabulator(RDataType type) {
			true
		}

		override isTabulated(RAttribute attr) {
			true
		}

		override toTabulatorJavaClass(RDataType type) {
			typeTranslator.toTabulatorJavaClass(type)
		}

		override getRule(RAttribute attr) {
			Optional.empty
		}

		override getFunction() {
			throw new UnsupportedOperationException("TODO: remove")
		}
	}
	
	@Inject RosettaTypeProvider typeProvider
	@Inject RosettaConfiguration rosettaConfiguration
	@Inject extension JavaTypeTranslator typeTranslator
	@Inject extension ImportManagerExtension
	
	@Inject extension RosettaEcoreUtil extensions
	@Inject extension ExternalAnnotationUtil
	@Inject extension ModelIdProvider
	@Inject extension RObjectFactory

	def generateTabulatorForReport(IFileSystemAccess2 fsa, RosettaReport report) {
		val tabulatorClass = report.toReportTabulatorJavaClass
		val topScope = new JavaScope(tabulatorClass.packageName)
		
		val inputType = report.reportType.buildRDataType
		val context = getReportTabulatorContext(inputType, Optional.ofNullable(report.ruleSource))
		val classBody = inputType.mainTabulatorClassBody(context, topScope, tabulatorClass)
		val content = buildClass(tabulatorClass.packageName, classBody, topScope)
		fsa.generateFile(tabulatorClass.canonicalName.withForwardSlashes + ".java", content)
	}
	
	def generateTabulatorForReportData(IFileSystemAccess2 fsa, RDataType type, Optional<RosettaExternalRuleSource> ruleSource) {
		val context = getReportTabulatorContext(type, ruleSource)
		if (context.needsTabulator(type)) {
			recursivelyGenerateTabulators(fsa, type, context, newHashSet)
		}
	}
	
	def generateTabulatorForData(IFileSystemAccess2 fsa, RDataType type) {
		if (type.isDataTabulatable) {
			val context = new DataTabulatorContext(typeTranslator)

			recursivelyGenerateTabulators(fsa, type, context, newHashSet)
		}
	}

	def generateTabulatorForFunction(IFileSystemAccess2 fsa, Function func) {
		if (func.isFunctionTabulatable) {
			val tabulatorClass = func.toApplicableTabulatorClass
			val topScope = new JavaScope(tabulatorClass.packageName)

			val t = typeProvider.getRTypeOfSymbol(func.output).RType
			val functionOutputType = if (t instanceof RChoiceType) {
				t.asRDataType
			} else {
				t
			}
			if (functionOutputType instanceof RDataType) {
				val context = createFunctionTabulatorContext(typeTranslator, func)
				
				val classBody = functionOutputType.mainTabulatorClassBody(context, topScope, tabulatorClass)
				val content = buildClass(tabulatorClass.packageName, classBody, topScope)
				fsa.generateFile(tabulatorClass.canonicalName.withForwardSlashes + ".java", content)
				
				recursivelyGenerateTabulators(fsa, functionOutputType, context, newHashSet)
			}
		}
	}

	private def void recursivelyGenerateTabulators(IFileSystemAccess2 fsa, RDataType type, TabulatorContext context, Set<RDataType> visited) {
		if (visited.add(type)) {
			val tabulatorClass = context.toTabulatorJavaClass(type)
			val topScope = new JavaScope(tabulatorClass.packageName)
			
			val classBody = type.tabulatorClassBody(context, topScope, tabulatorClass)
			val content = buildClass(tabulatorClass.packageName, classBody, topScope)
			fsa.generateFile(tabulatorClass.canonicalName.withForwardSlashes + ".java", content)
		
			type
				.allAttributes
				.map[RMetaAnnotatedType]
				.map[RType]
				.map[it instanceof RChoiceType ? asRDataType : it]
				.filter(RDataType)
				.forEach[recursivelyGenerateTabulators(fsa, it, context, visited)]
		}
	}
	
	private def ReportTabulatorContext getReportTabulatorContext(RDataType type, Optional<RosettaExternalRuleSource> ruleSource) {
		val ruleMap = newHashMap
		type.getAllReportingRules(ruleSource).forEach[key, rule| ruleMap.put(key.attr, rule)]
		new ReportTabulatorContext(extensions, typeTranslator, typeProvider, ruleMap, ruleSource)
	}
	
	private def boolean isFunctionTabulatable(Function func) {
		if (shouldGenerateLegacyTabulator) {
			return func.isAnnotatedWith("projection")
		} else {
			val annotations = rosettaConfiguration.generators.tabulators.annotations
			annotations.findFirst[func.isAnnotatedWith(it)] !== null
		}
	}
	
	private def boolean isDataTabulatable(RDataType type) {
		val types = rosettaConfiguration.generators.tabulators.types
		types.contains(type.symbolId.toString)
	}

	private def boolean isAnnotatedWith(Function func, String with) {
		func.annotations.findFirst[annotation.name == with] !== null
	}
	
	private def boolean shouldGenerateLegacyTabulator() {
		rosettaConfiguration.generators.tabulators.annotations.empty
	}
	
	private def TabulatorContext createFunctionTabulatorContext(JavaTypeTranslator typeTranslator, Function func) {
		shouldGenerateLegacyTabulator ? new ProjectionTabulatorContext(typeTranslator, func) : new FunctionTabulatorContext(typeTranslator, func)
	}
	
	private def JavaClass<Tabulator<?>> toApplicableTabulatorClass(Function func) {
		shouldGenerateLegacyTabulator ? func.toProjectionTabulatorJavaClass : func.toTabulatorJavaClass
	}
	
	private def StringConcatenationClient mainTabulatorClassBody(RDataType inputType, TabulatorContext context, JavaScope topScope, JavaClass<Tabulator<?>> tabulatorClass) {
		val inputClass = inputType.toJavaReferenceType
		
		val classScope = topScope.classScope(tabulatorClass.simpleName)
		
		val tabulateScope = classScope.methodScope("tabulate")
		val inputParam = tabulateScope.createUniqueIdentifier("input")
		
		if (context.needsTabulator(inputType)) {
			// There will be a tabulator available for `inputType`,
			// so we can inject it.
			val innerTabulatorClass = context.toTabulatorJavaClass(inputType)
			val innerTabulatorInstance = classScope.createUniqueIdentifier("tabulator")
			'''
			@«ImplementedBy»(«tabulatorClass».Impl.class)
			public interface «tabulatorClass» extends «Tabulator»<«inputClass»> {
				@«javax.inject.Singleton»
				class Impl implements «tabulatorClass» {
					private final «innerTabulatorClass» «innerTabulatorInstance»;

					@«javax.inject.Inject»
					public Impl(«innerTabulatorClass» «innerTabulatorInstance») {
						this.«innerTabulatorInstance» = «innerTabulatorInstance»;
					}

					@Override
					public «List»<«FieldValue»> tabulate(«inputClass» «inputParam») {
						return «innerTabulatorInstance».tabulate(«inputParam»);
					}
				}
			}
			'''
		} else {
			// There is no available tabulator for `inputType`,
			// so we generate a dummy implementation.
			'''
			@«ImplementedBy»(«tabulatorClass».Impl.class)
			public interface «tabulatorClass» extends «Tabulator»<«inputClass»> {
				@«javax.inject.Singleton»
				class Impl implements «tabulatorClass» {

					@Override
					public «List»<«FieldValue»> tabulate(«inputClass» «inputParam») {
						return «Arrays».asList();
					}
				}
			}
			'''
		}
	}
	
	private def StringConcatenationClient tabulatorClassBody(RDataType inputType, TabulatorContext context, JavaScope topScope, JavaClass<Tabulator<?>> tabulatorClass) {
		val inputClass = inputType.toJavaReferenceType
		
		val classScope = topScope.classScope(tabulatorClass.simpleName)
		findTabulatedFieldsAndCreateIdentifiers(inputType, context, classScope)
		val nestedTabulatorInstances = findNestedTabulatorsAndCreateIdentifiers(inputType, context, classScope)
		val tabulateScope = classScope.methodScope("tabulate")
		val inputParam = tabulateScope.createUniqueIdentifier("input")
		
		'''
		@«ImplementedBy»(«tabulatorClass».Impl.class)
		public interface «tabulatorClass» extends «Tabulator»<«inputClass»> {
			@«javax.inject.Singleton»
			class Impl implements «tabulatorClass» {
				«FOR attr : inputType.allAttributes»
					«IF context.isTabulated(attr)»
						«val fieldId = classScope.getIdentifierOrThrow(attr)»
						private final «Field» «fieldId»;
					«ENDIF»
				«ENDFOR»
				«IF !nestedTabulatorInstances.empty»

				«FOR tabInst : nestedTabulatorInstances»
					private final «context.toTabulatorJavaClass(tabInst.type)» «classScope.getIdentifierOrThrow(tabInst)»;
				«ENDFOR»
				«ENDIF»

				«IF !nestedTabulatorInstances.empty»@«javax.inject.Inject»«ENDIF»
				public Impl(«FOR tabInst : nestedTabulatorInstances SEPARATOR ", "»«context.toTabulatorJavaClass(tabInst.type)» «classScope.getIdentifierOrThrow(tabInst)»«ENDFOR») {
					«FOR tabInst : nestedTabulatorInstances»
						this.«classScope.getIdentifierOrThrow(tabInst)» = «classScope.getIdentifierOrThrow(tabInst)»;
					«ENDFOR»
					«initializeFields(inputType, context, classScope)»
				}

				@Override
				public «List»<«FieldValue»> tabulate(«inputClass» «inputParam») {
					«computeFieldValues(inputType, inputParam, context, tabulateScope)»
					return «fieldValuesAsList(inputType, context, tabulateScope)»;
				}
			}
		}
		'''
	}
	
	private def List<RAttribute> findTabulatedFieldsAndCreateIdentifiers(RDataType type, TabulatorContext context, JavaScope scope) {
		type
			.allAttributes
			.filter[context.isTabulated(it)]
			.map[
				scope.createIdentifier(it, name + "Field")
				it
			].toList
	}
	private def StringConcatenationClient initializeFields(RDataType type, TabulatorContext context, JavaScope scope) {
		'''
		«FOR attr : type.allAttributes»
			«IF context.isTabulated(attr)»
				«val fieldId = scope.getIdentifierOrThrow(attr)»
				«val rule = context.getRule(attr)»
				this.«fieldId» = new «FieldImpl»(
					"«StringEscapeUtils.escapeJava(attr.name)»",
					«attr.isMulti»,
					«rule.map[symbolId.toModelSymbolCode].toOptionalCode»,
					«rule.map[identifier].map['"' + it + '"'].toOptionalCode»,
					«Arrays».asList()
				);
			«ENDIF»
		«ENDFOR»
		'''
	}
	
	private def Set<NestedTabulatorInstance> findNestedTabulatorsAndCreateIdentifiers(RDataType type, TabulatorContext context, JavaScope scope) {
		val result = type.allAttributes
			.filter[context.isTabulated(it)]
			.map[RMetaAnnotatedType]
			.map[RType]
			.map[it instanceof RChoiceType ? asRDataType : it]
			.filter(RDataType)
			.map[toNestedTabulatorInstance]
			.toSet
		result.forEach[scope.createIdentifier(it, context.toTabulatorJavaClass(it.type).simpleName.toFirstLower)]
		result
	}
	
	private def StringConcatenationClient computeFieldValues(RDataType type, GeneratedIdentifier inputParam, TabulatorContext context, JavaScope scope) {
		'''
		«FOR attr : type.allAttributes»
			«IF context.isTabulated(attr)»
				«fieldValue(type.toJavaReferenceType, attr, inputParam, scope)»
			«ENDIF»
		«ENDFOR»
		'''
	}

	private def StringConcatenationClient fieldValue(JavaPojoInterface javaType, RAttribute attr, GeneratedIdentifier inputParam, JavaScope scope) {
		val rawAttr = attr.RMetaAnnotatedType.RType
		val rType = if (rawAttr instanceof RChoiceType) {
			rawAttr.asRDataType
		} else {
			rawAttr
		}
			
		val resultId = scope.createIdentifier(attr.toComputedField, attr.name)
		
		val lambdaScope = scope.lambdaScope
		val lambdaParam = lambdaScope.createUniqueIdentifier("x")
		
		val nestedLambdaScope = lambdaScope.lambdaScope
		val nestedLambdaParam = nestedLambdaScope.createUniqueIdentifier("x")
		
		val getter = javaType.findProperty(attr.name).getterName
		if (rType instanceof RDataType) {
			val nestedTabulator = scope.getIdentifierOrThrow(rType.toNestedTabulatorInstance)
			'''
			«FieldValue» «resultId» = «Optional».ofNullable(«inputParam».«getter»())
				«IF attr.isMulti»
				.map(«lambdaParam» -> «lambdaParam».stream()
					«IF attr.RMetaAnnotatedType.hasMeta»
						.map(«nestedLambdaParam» -> «nestedLambdaParam».getValue())
						.filter(«Objects»::nonNull)
					«ENDIF»
					.map(«nestedLambdaParam» -> «nestedTabulator».tabulate(«nestedLambdaParam»))
					.collect(«Collectors».toList()))
				.map(fieldValues -> new «MultiNestedFieldValueImpl»(«scope.getIdentifierOrThrow(attr)», Optional.of(fieldValues)))
				.orElse(new «MultiNestedFieldValueImpl»(«scope.getIdentifierOrThrow(attr)», Optional.empty()));
				«ELSE»
				«IF attr.RMetaAnnotatedType.hasMeta».map(«lambdaParam» -> «lambdaParam».getValue())«ENDIF»
				.map(«lambdaParam» -> new «NestedFieldValueImpl»(«scope.getIdentifierOrThrow(attr)», Optional.of(«nestedTabulator».tabulate(«lambdaParam»))))
				.orElse(new «NestedFieldValueImpl»(«scope.getIdentifierOrThrow(attr)», Optional.empty()));
				«ENDIF»
			'''
		} else {
			'''
			«IF !attr.RMetaAnnotatedType.hasMeta»
			«FieldValue» «resultId» = new «FieldValueImpl»(«scope.getIdentifierOrThrow(attr)», «Optional».ofNullable(«inputParam».«getter»()));
			«ELSEIF attr.isMulti»
			«FieldValue» «resultId» = new «FieldValueImpl»(«scope.getIdentifierOrThrow(attr)», «Optional».ofNullable(«inputParam».«getter»())
				.map(«lambdaParam» -> «lambdaParam».stream()
					.map(«nestedLambdaParam» -> «nestedLambdaParam».getValue())
					.filter(«Objects»::nonNull)
					.collect(«Collectors».toList())));
			«ELSE»
			«FieldValue» «resultId» = new «FieldValueImpl»(«scope.getIdentifierOrThrow(attr)», «Optional».ofNullable(«inputParam».«getter»())
				.map(«lambdaParam» -> «lambdaParam».getValue()));
			«ENDIF»
			'''
		}
	}
	
	private def StringConcatenationClient fieldValuesAsList(RDataType type, TabulatorContext context, JavaScope scope) {
		'''
		«Arrays».asList(
			«FOR attr : type.allAttributes.filter[context.isTabulated(it)] SEPARATOR ","»
			«scope.getIdentifier(attr.toComputedField)»
			«ENDFOR»
		)'''
	}
	
	private def StringConcatenationClient toOptionalCode(Optional<?> object) {
		if (object.isPresent) {
			'''«Optional».of(«object.get»)'''
		} else {
			'''«Optional».empty()'''
		}
	}
	private def StringConcatenationClient toDottedPathCode(DottedPath path) {
		'''«DottedPath».of("«path.withSeparator("\", \"")»")'''
	}
	private def StringConcatenationClient toModelSymbolCode(ModelSymbolId symbolId) {
		'''new «ModelSymbolId»(«symbolId.namespace.toDottedPathCode», "«symbolId.name»")'''
	}
	
	private def toNestedTabulatorInstance(RDataType type) {
		new NestedTabulatorInstance(type)
	}
	@org.eclipse.xtend.lib.annotations.Data
	private static class NestedTabulatorInstance {
		RDataType type
	}
	private def toComputedField(RAttribute attr) {
		new ComputedField(attr)
	}
	@org.eclipse.xtend.lib.annotations.Data
	private static class ComputedField {
		RAttribute attribute
	}
}