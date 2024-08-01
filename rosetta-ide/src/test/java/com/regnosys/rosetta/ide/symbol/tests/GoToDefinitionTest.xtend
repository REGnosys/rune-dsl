package com.regnosys.rosetta.ide.symbol.tests

import org.junit.jupiter.api.Test
import com.regnosys.rosetta.ide.tests.AbstractRosettaLanguageServerTest

class GoToDefinitionTest extends AbstractRosettaLanguageServerTest {	
	@Test
	def testGoToTranslateDefinition() {
		testDefinition[
			val model = '''
				namespace foo.bar
				
				type Foo:
					a string (1..1)
				
				type Bar:
					b Qux (1..1)
				
				type Qux:
					c string (1..1)
				
				translate source FooBar {
				    translate Bar to Foo {
				    	a: translate b, 42 to string
				    }
				    
				    translate qux Qux, context number to string:
				    	qux -> c + context to-string
				}
			'''
			it.model = model
			it.line = 13
			it.column = 8
			it.expectedDefinitions = '''
				MyModel.rosetta [[16, 4] .. [16, 13]]
				'''
		]
	}
	
	@Test
	def testGoToTranslateDefinitionInOtherNamespace() {
		testDefinition[
			val model = '''
				namespace foo.bar.other
				
				import foo.bar.*
				
				func DoTheThing:
					output:
						result string (1..1)
					set result:
						translate Qux { c: "value" }, 42 to string using FooBar
			'''
			it.filesInScope = #{
				"OtherModel.rosetta" -> '''
					namespace foo.bar
					
					type Foo:
						a string (1..1)
					
					type Bar:
						b Qux (1..1)
					
					type Qux:
						c string (1..1)
					
					translate source FooBar {
					    translate Bar to Foo {
					    	a: translate b, 42 to string
					    }
					    
					    translate qux Qux, context number to string:
					    	qux -> c + context to-string
					}
					'''
			}
			it.model = model
			it.line = 8
			it.column = 2
			it.expectedDefinitions = '''
				OtherModel.rosetta [[16, 4] .. [16, 13]]
				'''
		]
	}
}
