/*
 * generated by Xtext 2.10.0
 */
package com.regnosys.rosetta.generator.java.object

import com.regnosys.rosetta.rosetta.RosettaEnumeration
import com.regnosys.rosetta.tests.RosettaTestInjectorProvider
import com.regnosys.rosetta.tests.util.ModelHelper
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.extensions.InjectionExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.^extension.ExtendWith

import static org.junit.jupiter.api.Assertions.*
import jakarta.inject.Inject

@ExtendWith(InjectionExtension)
@InjectWith(RosettaTestInjectorProvider)
class RosettaModelTest{

	@Inject extension ModelHelper modelHelper
	
	@Test
	def void testEnumeration() {
		val model =
		'''			
			enum QuoteRejectReasonEnum: <"The enumeration values.">
			[synonym ISO value "QuoteRejectReason" componentID 24]
				UnknownSymbol <"unknown symbol">
				[synonym ISO_20022 value "UK" definition "Unknown Symbol"]
				KnownSymbol
				
			synonym source ISO
			synonym source ISO_20022
		'''.parseRosettaWithNoErrors
		
		val enum = model.elements.get(0) as RosettaEnumeration
		assertEquals("QuoteRejectReasonEnum", enum.name)
		assertEquals("The enumeration values.", enum.definition)
		
		val synonyms = enum.synonyms.get(0)
		assertEquals("ISO", synonyms.sources.head.getName())
		assertEquals("QuoteRejectReason", synonyms.body.values.get(0).getName())
		assertEquals("componentID", synonyms.body.values.get(0).refType.getName())
		assertEquals(24, synonyms.body.values.get(0).value)
		
		val enumValues1 = enum.enumValues.get(0)
		assertEquals("UnknownSymbol", enumValues1.name)
		assertEquals("unknown symbol", enumValues1.definition)
		
		val enumSynonyms = enumValues1.enumSynonyms.get(0)
		assertEquals("ISO_20022", enumSynonyms.sources.map[it.getName].join)

		assertEquals("UK", enumSynonyms.synonymValue)
		assertEquals("Unknown Symbol", enumSynonyms.definition)
		
		val enumValues2 = enum.enumValues.get(1) 
		assertEquals("KnownSymbol", enumValues2.name)
	}
}
