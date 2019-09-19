package com.regnosys.rosetta.generator.java.function

import com.google.inject.ImplementedBy
import com.google.inject.Inject
import com.google.inject.Provider
import com.regnosys.rosetta.generator.java.expression.Context
import com.regnosys.rosetta.generator.java.expression.ExpressionGeneratorWithBuilder
import com.regnosys.rosetta.generator.java.expression.RosettaExpressionJavaGeneratorForFunctions
import com.regnosys.rosetta.generator.java.expression.RosettaExpressionJavaGeneratorForFunctions.ParamMap
import com.regnosys.rosetta.generator.java.util.ImportManagerExtension
import com.regnosys.rosetta.generator.java.util.JavaNames
import com.regnosys.rosetta.generator.java.util.JavaType
import com.regnosys.rosetta.generator.util.RosettaFunctionExtensions
import com.regnosys.rosetta.generator.util.Util
import com.regnosys.rosetta.rosetta.RosettaCallableWithArgs
import com.regnosys.rosetta.rosetta.RosettaEnumeration
import com.regnosys.rosetta.rosetta.RosettaFeature
import com.regnosys.rosetta.rosetta.RosettaRegularAttribute
import com.regnosys.rosetta.rosetta.simple.AssignPathRoot
import com.regnosys.rosetta.rosetta.simple.Attribute
import com.regnosys.rosetta.rosetta.simple.Condition
import com.regnosys.rosetta.rosetta.simple.Function
import com.regnosys.rosetta.rosetta.simple.Operation
import com.regnosys.rosetta.rosetta.simple.Segment
import com.regnosys.rosetta.rosetta.simple.ShortcutDeclaration
import com.regnosys.rosetta.types.RosettaTypeProvider
import com.regnosys.rosetta.utils.ExpressionHelper
import com.rosetta.model.lib.functions.Mapper
import com.rosetta.model.lib.functions.MapperBuilder
import com.rosetta.model.lib.functions.MapperS
import com.rosetta.model.lib.functions.RosettaFunction
import java.util.List
import java.util.Map
import org.eclipse.xtend2.lib.StringConcatenationClient
import org.eclipse.xtext.generator.IFileSystemAccess2

import static com.regnosys.rosetta.generator.java.util.ModelGeneratorUtil.*

class FuncGenerator {

	@Inject RosettaExpressionJavaGeneratorForFunctions expressionGenerator
	@Inject ExpressionGeneratorWithBuilder expressionWithBuilder
	@Inject RosettaFunctionDependencyProvider functionDependencyProvider
	@Inject RosettaTypeProvider typeProvider
	@Inject extension RosettaFunctionExtensions
	@Inject ExpressionHelper exprHelper
	@Inject extension ImportManagerExtension

	def void generate(JavaNames javaNames, IFileSystemAccess2 fsa, Function func, String version) {
		val fileName = javaNames.packages.functions.directoryName + '/' + func.name + '.java'

		
		val dependencies = collectFunctionDependencies(func)

		val classBody = if (func.handleAsEnumFunction) {
				tracImports(func.dispatchClassBody(func.name, dependencies, javaNames, version))
			} else {
				tracImports(func.classBody(func.name, dependencies, javaNames, version, false))
			}
		val content = '''
			package «javaNames.packages.functions.packageName»;
			
			«FOR imp : classBody.imports»
				import «imp»;
			«ENDFOR»
			
			«FOR imp : classBody.staticImports»
				import static «imp»;
			«ENDFOR»
			
			«classBody.toString»
		'''
		fsa.generateFile(fileName, content)
	}
	
	private def collectFunctionDependencies(Function func) {
		val deps = func.shortcuts.flatMap[functionDependencyProvider.functionDependencies(it.expression)] +
			func.operations.flatMap[functionDependencyProvider.functionDependencies(it.expression)]
		val condDeps = (func.conditions + func.postConditions).flatMap[expressions].flatMap [
			functionDependencyProvider.functionDependencies(it)
		]
		return Util.distinctBy(deps + condDeps, [name]).sortBy[it.name]
	}

	private def StringConcatenationClient classBody(Function func, String className,
		Iterable<? extends RosettaCallableWithArgs> dependencies, extension JavaNames names, String version, boolean isStatic) {
		val isAbstract = func.operations.nullOrEmpty
		val outputName = getOutput(func)?.name
		val outputType = func.outputTypeOrVoid(names)
		val aliasOut = func.shortcuts.toMap([it], [exprHelper.usesOutputParameter(it.expression)])
		val outNeedsBuilder = expressionWithBuilder.needsBuilder(getOutput(func))
		'''
			«IF isAbstract»@«ImplementedBy»(«className»Impl.class)«ENDIF»
			public «IF isStatic»static «ENDIF»«IF isAbstract»abstract «ENDIF»class «className» implements «RosettaFunction» {
				«IF !dependencies.empty»
					
					// RosettaFunction dependencies
					//
				«ENDIF»
				«FOR dep : dependencies»
					@«Inject» protected «dep.toJavaQualifiedType» «dep.name.toFirstLower»;
				«ENDFOR»
			
				/**
				«FOR input : getInputs(func)»
					* @param «input.name» «input.definition»
				«ENDFOR»
				«IF getOutput(func) !== null»
					* @return «outputName» «getOutput(func).definition»
				«ENDIF»
				*/
				public «outputType» evaluate(«func.inputsAsParameters(names)») {
					«IF !func.conditions.empty»
						// pre-conditions
						«FOR cond:func.conditions»
						
							«cond.contributeCondition»
						«ENDFOR»
					«ENDIF»
					
					«outputType» «outputName» = doEvaluate(«func.inputsAsArguments(names)»)«IF outNeedsBuilder».build()«ENDIF»;
					
					«IF !func.postConditions.empty»
						// post-conditions
						«FOR cond:func.postConditions»

							«cond.contributeCondition»
						«ENDFOR»
					«ENDIF»
					return «outputName»;
				}
				
				«IF isAbstract»
					protected abstract «getOutput(func).toBuilderType(names)» doEvaluate(«func.inputsAsParameters(names)»);
				«ELSE»
					protected «getOutput(func).toBuilderType(names)» doEvaluate(«func.inputsAsParameters(names)») {
						«IF getOutput(func) !== null»
							«getOutput(func).toHolderType(names)» «outputName»Holder = «IF outNeedsBuilder»«getOutput(func).toJavaQualifiedType».builder()«ELSE»null«ENDIF»;
						«ENDIF»
						«FOR indexed : func.operations.indexed»
							«indexed.value.assign(aliasOut, names)»;
						«ENDFOR»
						return «outputName»Holder«IF !outNeedsBuilder».get()«ENDIF»;
					}
					
				«ENDIF»
				«FOR alias : func.shortcuts»
					
					«IF aliasOut.get(alias)»
						protected «names.shortcutJavaType(alias)» «alias.name»(«getOutput(func).toBuilderType(names)» «outputName», «IF !getInputs(func).empty»«func.inputsAsParameters(names)»«ENDIF») {
							return «expressionWithBuilder.toJava(alias.expression, Context.create(names))»;
						}
					«ELSE»
						protected «IF expressionWithBuilder.needsBuilder(alias)»«MapperBuilder»«ELSE»«Mapper»«ENDIF»<«toJavaType(typeProvider.getRType(alias.expression))»> «alias.name»(«func.inputsAsParameters(names)») {
							return «expressionGenerator.javaCode(alias.expression, new ParamMap)»;
						}
					«ENDIF»
				«ENDFOR»
			}
		'''
	}

	
	def private StringConcatenationClient dispatchClassBody(Function function,String className, Iterable<? extends RosettaCallableWithArgs> dependencies, extension JavaNames names, String version) {
		val dispatchingFuncs = function.dispatchingFunctions.sortBy[name].toList
		val enumParam = function.inputs.filter[type instanceof RosettaEnumeration].head.name
		val outputType = function.outputTypeOrVoid(names)
		'''
		«emptyJavadocWithVersion(version)»
		public class «className» {
			«FOR dep : dependencies»
				@«Inject» protected «dep.toJavaQualifiedType» «dep.name.toFirstLower»;
			«ENDFOR»
			
			«FOR enumFunc : dispatchingFuncs»
				@«Inject» protected «toTargetClassName(enumFunc)» «toTargetClassName(enumFunc).lastSegment»;
			«ENDFOR»
			
			public «outputType» evaluate(«function.inputsAsParameters(names)») {
				switch («enumParam») {
					«FOR enumFunc : dispatchingFuncs»
						«val enumValClass = toTargetClassName(enumFunc).lastSegment»
						case «enumValClass»:
							return «enumValClass».evaluate(«function.inputsAsArguments(names)»);
					«ENDFOR»
					default:
						throw new IllegalArgumentException("Enum value not implemented: " + «enumParam»);
				}
			}
			
			«FOR enumFunc : dispatchingFuncs»
			
			«val enumValClass = toTargetClassName(enumFunc).lastSegment»
			«enumFunc.classBody(enumValClass, collectFunctionDependencies(enumFunc), names,  version, true)»
			«ENDFOR»
		}'''
	}
	
	
	private def StringConcatenationClient assign(Operation operation, Map<ShortcutDeclaration, Boolean> outs,
		JavaNames names) {
		val pathAsList = operation.path.asSegmentList
		val ctx = Context.create(names)
		if (pathAsList.isEmpty)
			'''
			«IF expressionWithBuilder.needsBuilder(operation.assignRoot)»
				«operation.assignTarget(outs, names)»
					.«IF operation.assignRoot.isMany»add«ELSE»set«ENDIF»«operation.assignRoot.name.toFirstUpper»(«expressionWithBuilder.toJava(operation.expression, ctx)»)
			«ELSE»
				«operation.assignTarget(outs, names)» = «MapperS».of(«expressionWithBuilder.toJava(operation.expression, ctx)»)«ENDIF»'''
		else
			'''
				«operation.assignTarget(outs, names)»
					«FOR seg : pathAsList»«IF seg.next !== null».getOrCreate«seg.attribute.name.toFirstUpper»(«IF seg.attribute.many»«seg.index»«ENDIF»)«ELSE»
					.«IF seg.attribute.isMany»add«ELSE»set«ENDIF»«seg.attribute.name.toFirstUpper»(«expressionGenerator.javaCode(operation.expression, new ParamMap)».get())«ENDIF»«ENDFOR»;
			'''
	}

	private def StringConcatenationClient assignTarget(Operation operation, Map<ShortcutDeclaration, Boolean> outs,
		JavaNames names) {
		val root = operation.assignRoot
		switch (root) {
			Attribute: '''«root.name»Holder'''
			ShortcutDeclaration: '''«root.name»(«IF outs.get(root)»«getOutput(operation.function)?.name»Holder«IF !getInputs(operation.function).empty», «ENDIF»«ENDIF»«inputsAsArguments(operation.function, names)»)'''
		}
	}

	private def StringConcatenationClient contributeCondition(Condition condition) {
		'''
			assert
				«FOR expr : condition.expressions SEPARATOR ' &&'» 
					«expressionGenerator.javaCode(expr, null)».get()
				«ENDFOR»
				: "«condition.definition»";
		'''
	}

	private def JavaType outputTypeOrVoid(Function function, extension JavaNames names) {
		val out = getOutput(function)
		if (out === null) {
			JavaType.create('void')
		} else {
			out.type.toJavaType()
		}
	}

	private def StringConcatenationClient inputsAsArguments(extension Function function, extension JavaNames names) {
		'''«FOR input : getInputs(function) SEPARATOR ', '»«input.name»«ENDFOR»'''
	}

	private def StringConcatenationClient inputsAsParameters(extension Function function, extension JavaNames names) {
		'''«FOR input : getInputs(function) SEPARATOR ', '»«input.toJavaQualifiedType()» «input.name»«ENDFOR»'''
	}

	def private StringConcatenationClient shortcutJavaType(JavaNames names, ShortcutDeclaration feature) {
		val rType = typeProvider.getRType(feature.expression)
		val javaType = names.toJavaType(rType)
		'''«javaType»«IF expressionWithBuilder.needsBuilder(rType)».«javaType»Builder«ENDIF»'''
	}

	private def StringConcatenationClient toBuilderType(Attribute attr, JavaNames names) {
		val javaType = names.toJavaType(attr.type)
		'''«IF expressionWithBuilder.needsBuilder(attr)»«javaType».«javaType»Builder«ELSE»«javaType»«ENDIF»'''
	}
	
	private def StringConcatenationClient toHolderType(Attribute attr, JavaNames names) {
		val javaType = names.toJavaType(attr.type)
		'''«IF expressionWithBuilder.needsBuilder(attr)»«javaType».«javaType»Builder«ELSE»«Mapper»<«javaType»>«ENDIF»'''
	}
	
	private def List<Segment> asSegmentList(Segment segment) {
		val result = newArrayList
		if (segment !== null) {
			result.add(segment)
			val segmentNext = segment?.next
			if (segmentNext !== null) {
				result.addAll(asSegmentList(segmentNext))
			}
		}
		return result
	}

	private def isMany(AssignPathRoot root) {
		switch (root) {
			Attribute: root.card.isMany
		}
	}

	private def isMany(RosettaFeature feature) {
		switch (feature) {
			RosettaRegularAttribute: feature.card.isMany
			Attribute: feature.card.isMany
			default: throw new IllegalStateException('Unsupported type passed ' + feature?.eClass?.name)
		}
	}
}
