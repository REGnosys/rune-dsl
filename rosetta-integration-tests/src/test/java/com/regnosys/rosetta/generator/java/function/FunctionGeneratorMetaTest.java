package com.regnosys.rosetta.generator.java.function;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import javax.inject.Inject;

import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.google.common.collect.Lists;
import com.regnosys.rosetta.generator.java.RosettaJavaPackages;
import com.regnosys.rosetta.tests.RosettaTestInjectorProvider;
import com.regnosys.rosetta.tests.util.CodeGeneratorTestHelper;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.meta.FieldWithMeta;
import com.rosetta.model.lib.meta.Reference;
import com.rosetta.model.lib.meta.ReferenceWithMeta;
import com.rosetta.model.metafields.MetaFields;

@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaTestInjectorProvider.class)
public class FunctionGeneratorMetaTest {

    @Inject
    FunctionGeneratorHelper functionGeneratorHelper;
    @Inject
    CodeGeneratorTestHelper generatorTestHelper;

    @Test
    void canSetMetaFieldsUsingWithMetaSyntax() {
        var model = """
        metaType id string
        metaType scheme string
  
        func MyFunc:
            output:
                result string (1..1)
                  [metadata scheme]
                  [metadata id]
            set result: "someValue" with-meta {
                                        scheme: "someScheme",
                                        id: "someId"
                                    }
  """;

        // syntax
        // scoping
        // validation: meta field is correct type, with meta value cardinality check (someScheme should be single), lhs of with-meta should be single cardinality
        // typeProvider: return type of with-meta
        // cardinalityProvider: returned cardinality of with-meta, has to be single
        // expectedTypeProvider: is lhs (someValue) correct type given result type and addition of meta to lhs
        // java layer: code generation (expression generator)

        // formatting: RosettaExpressionFormatter
        // syntax highlighting: textmate (in case you need it but probably not RosettaSemanticTokensService)

        var code = generatorTestHelper.generateCode(model);
        
        generatorTestHelper.writeClasses(code, "canSetMetaFieldsUsingWithMetaSyntax");

        var classes = generatorTestHelper.compileToClasses(code);

    }

    @Test
    void canSetEnumOnReferenceWithMeta() {
        var model = """	  
        metaType reference string
          
        enum MyEnum:
            A
            B
            C
                		    	    
		type Foo:
		    myEnumField MyEnum (1..1)
		     [metadata reference]
		
		func MyFunc:
		    output:
		        foo Foo (1..1)
		        
		    set foo -> myEnumField: MyEnum -> B
        """;


        var code = generatorTestHelper.generateCode(model);

        var classes = generatorTestHelper.compileToClasses(code);

        var myFunc = functionGeneratorHelper.createFunc(classes, "MyFunc");

        var result = functionGeneratorHelper.invokeFunc(myFunc, RosettaModelObject.class);

        var expected = generatorTestHelper.createInstanceUsingBuilder(classes, new RosettaJavaPackages.RootPackage("com.rosetta.test.model"), "Foo", Map.of(
				"myEnumField", generatorTestHelper.createInstanceUsingBuilder(classes, new RosettaJavaPackages.RootPackage("com.rosetta.test.model.metafields"), "ReferenceWithMetaMyEnum", Map.of(
						"value", generatorTestHelper.createEnumInstance(classes, "MyEnum", "B")
					))

		 ));

        assertEquals(expected, result);
    }

    @Test
    void canSetEnumOnFieldWithMeta() {
        var model = """	   
        enum MyEnum:
            A
            B
            C
                		    	    
		type Foo:
		    myEnumField MyEnum (1..1)
		     [metadata scheme]
		
		func MyFunc:
		    output:
		        foo Foo (1..1)
		        
		    set foo -> myEnumField: MyEnum -> B
        """;


        var code = generatorTestHelper.generateCode(model);

        var classes = generatorTestHelper.compileToClasses(code);

        var myFunc = functionGeneratorHelper.createFunc(classes, "MyFunc");

        var result = functionGeneratorHelper.invokeFunc(myFunc, RosettaModelObject.class);

        var expected = generatorTestHelper.createInstanceUsingBuilder(classes, new RosettaJavaPackages.RootPackage("com.rosetta.test.model"), "Foo", Map.of(
				"myEnumField", generatorTestHelper.createInstanceUsingBuilder(classes, new RosettaJavaPackages.RootPackage("com.rosetta.test.model.metafields"), "FieldWithMetaMyEnum", Map.of(
						"value", generatorTestHelper.createEnumInstance(classes, "MyEnum", "B")
					))

		 ));

        assertEquals(expected, result);
    }

    @Test
    void canSetSingleCardinalityMetaToListOfMetaUsingConstructor() {
        var model = """
        metaType reference string
        metaType key string
  
        type Foo:
            barReferences Bar (0..*)
            [metadata reference]
		
        type Bar:
            [metadata key]
            barField string (0..1)
		
        func MyFunc:
            inputs:
                bar Bar (0..1)
            output:
                foo Foo (0..1)
		
            set foo: Foo {
                barReferences: bar
            }
  """;

        var code = generatorTestHelper.generateCode(model);

        var classes = generatorTestHelper.compileToClasses(code);

        var bar = generatorTestHelper.createInstanceUsingBuilder(classes, new RosettaJavaPackages.RootPackage("com.rosetta.test.model"), "Bar", Map.of(
					"barField", "barFieldValue",
					"meta", MetaFields.builder().setExternalKey("someExternalKey").build()
       		 ));

        var myFunc = functionGeneratorHelper.createFunc(classes, "MyFunc");

        var result = functionGeneratorHelper.invokeFunc(myFunc, RosettaModelObject.class, bar);

        var expected =  generatorTestHelper.createInstanceUsingBuilder(classes, new RosettaJavaPackages.RootPackage("com.rosetta.test.model"), "Foo", Map.of(
				"barReferences", Lists.newArrayList(generatorTestHelper.createInstanceUsingBuilder(classes, new RosettaJavaPackages.RootPackage("com.rosetta.test.model.metafields"), "ReferenceWithMetaBar", Map.of(
							"value", generatorTestHelper.createInstanceUsingBuilder(classes, new RosettaJavaPackages.RootPackage("com.rosetta.test.model"), "Bar", Map.of(
									"barField", "barFieldValue",
									"meta", MetaFields.builder().setExternalKey("someExternalKey").build()
				       		 ))

						)))

   		 ));

        assertEquals(expected, result);
    }

    @Test
    void canSetSingleCardinalityMetaToListOfMeta() {
        var model = """
        metaType reference string
        metaType key string
  
        type Foo:
            barReferences Bar (0..*)
            [metadata reference]
		
        type Bar:
            [metadata key]
            barField string (0..1)
		
        func MyFunc:
            inputs:
                bar Bar (0..1)
            output:
                foo Foo (0..1)
		
            set foo -> barReferences: bar
  """;

        var code = generatorTestHelper.generateCode(model);

        var classes = generatorTestHelper.compileToClasses(code);

        var bar = generatorTestHelper.createInstanceUsingBuilder(classes, new RosettaJavaPackages.RootPackage("com.rosetta.test.model"), "Bar", Map.of(
					"barField", "barFieldValue",
					"meta", MetaFields.builder().setExternalKey("someExternalKey").build()
       		 ));

        var myFunc = functionGeneratorHelper.createFunc(classes, "MyFunc");

        var result = functionGeneratorHelper.invokeFunc(myFunc, RosettaModelObject.class, bar);

        var expected =  generatorTestHelper.createInstanceUsingBuilder(classes, new RosettaJavaPackages.RootPackage("com.rosetta.test.model"), "Foo", Map.of(
				"barReferences", Lists.newArrayList(generatorTestHelper.createInstanceUsingBuilder(classes, new RosettaJavaPackages.RootPackage("com.rosetta.test.model.metafields"), "ReferenceWithMetaBar", Map.of(
							"value", generatorTestHelper.createInstanceUsingBuilder(classes, new RosettaJavaPackages.RootPackage("com.rosetta.test.model"), "Bar", Map.of(
									"barField", "barFieldValue",
									"meta", MetaFields.builder().setExternalKey("someExternalKey").build()
				       		 ))

						)))

   		 ));

        assertEquals(expected, result);
    }

    @Test
    void canSetNestedCombinedFieldWithMetaUsingConstructor() {
        var model = """
        type Foo:
           fooField string (1..1)
               [metadata scheme]
		
        func MyFunc:
            inputs:
              myInput string (1..1)
                 [metadata scheme]
        
            output:
                result Foo (1..1)
        
            set result:
                Foo {
                  fooField: myInput
                }
        """;
        
        var code = generatorTestHelper.generateCode(model);
         
        var classes = generatorTestHelper.compileToClasses(code);
        var myFunc = functionGeneratorHelper.createFunc(classes, "MyFunc");

        var result = functionGeneratorHelper.invokeFunc(myFunc, RosettaModelObject.class, generatorTestHelper.createFieldWithMetaString(classes, "someValue", "someScheme"));

        var expected = generatorTestHelper.createInstanceUsingBuilder(classes, new RosettaJavaPackages.RootPackage("com.rosetta.test.model"), "Foo", Map.of(
                "fooField", generatorTestHelper.createFieldWithMetaString(classes, "someValue", "someScheme")
        ));

        assertEquals(expected, result);    	
    }    
    
    @Test
    void canSetNestedCombinedFieldWithMeta() {
        var model = """
        type Foo:
           fooField string (1..1)
               [metadata scheme]
		
        func MyFunc:
            inputs:
              myInput string (1..1)
                 [metadata scheme]
        
            output:
                result Foo (1..1)
        
            set result -> fooField: myInput
        """;
        
        var code = generatorTestHelper.generateCode(model);
 
        var classes = generatorTestHelper.compileToClasses(code);
        var myFunc = functionGeneratorHelper.createFunc(classes, "MyFunc");

        var result = functionGeneratorHelper.invokeFunc(myFunc, RosettaModelObject.class, generatorTestHelper.createFieldWithMetaString(classes, "someValue", "someScheme"));

        var expected = generatorTestHelper.createInstanceUsingBuilder(classes, new RosettaJavaPackages.RootPackage("com.rosetta.test.model"), "Foo", Map.of(
                "fooField", generatorTestHelper.createFieldWithMetaString(classes, "someValue", "someScheme")
        ));

        assertEquals(expected, result);    	
    }
    
    @Test
    void canSetMetaOutputWhereInputArgumentIsNull() {
        var model = """
        func MyFunc:
            inputs:
                value string (0..1)
                scheme string (0..1)
            output:
                result string (0..1)
                    [metadata scheme]
		
            set result: value
            set result -> scheme: scheme
  """;
        
       var code = generatorTestHelper.generateCode(model);
              
       var classes = generatorTestHelper.compileToClasses(code);
       var myFunc = functionGeneratorHelper.createFunc(classes, "MyFunc");

       var result = functionGeneratorHelper.invokeFunc(myFunc, FieldWithMeta.class, null, null);
       
       var expected = generatorTestHelper.createInstanceUsingBuilder(classes, new RosettaJavaPackages.RootPackage("com.rosetta.model.metafields"), "FieldWithMetaString", Map.of());
       
       assertEquals(expected, result);    	
    }
    
    @Test
    void canSetMetaLocationOnFunctionObjectOutput() {
        var model = """
        metaType location string
       
        type Foo:
            field string (1..1)
       
        func MyFunc:
            output:
                result Foo (1..1)
                    [metadata location]
            set result -> field:  "someValue"
            set result -> location: "someAddress"
       """;

        var code = generatorTestHelper.generateCode(model);
        
        var classes = generatorTestHelper.compileToClasses(code);
        var myFunc = functionGeneratorHelper.createFunc(classes, "MyFunc");

        var result = functionGeneratorHelper.invokeFunc(myFunc, FieldWithMeta.class);
        
        var expected = generatorTestHelper.createInstanceUsingBuilder(classes, new RosettaJavaPackages.RootPackage("com.rosetta.test.model.metafields"), "FieldWithMetaFoo", Map.of(
                "value", generatorTestHelper.createInstanceUsingBuilder(classes, new RosettaJavaPackages.RootPackage("com.rosetta.test.model"), "Foo", Map.of(
            			"field", "someValue"
            		)),
                "meta", MetaFields.builder().setLocation("someAddress")
        ));

        assertEquals(expected, result);    	
    }   
        
    @Test
    void canSetMetaLocationOnFunctionBasicOutput() {
        var model = """
        metaType location string

        func MyFunc:
            output:
                result string (1..1)
                    [metadata location]
            set result:  "someValue"
            set result -> location: "someAddress"
       """;

        var code = generatorTestHelper.generateCode(model);
        
        var classes = generatorTestHelper.compileToClasses(code);
        var myFunc = functionGeneratorHelper.createFunc(classes, "MyFunc");

        var result = functionGeneratorHelper.invokeFunc(myFunc, FieldWithMeta.class);
        
        var expected = generatorTestHelper.createInstanceUsingBuilder(classes, new RosettaJavaPackages.RootPackage("com.rosetta.model.metafields"), "FieldWithMetaString", Map.of(
                "value", "someValue",
                "meta", MetaFields.builder().setLocation("someAddress")
        ));

        assertEquals(expected, result);    	
    }
    
    @Test
    void canSetMetaAddressOnFunctionObjectOutput() {
        var model = """
        metaType address string
       
        type Foo:
            field string (1..1)

        func MyFunc:
            output:
                result Foo (1..1)
                    [metadata address]
            set result -> field:  "someValue"
            set result -> address: "someLocation"
       """;

        var code = generatorTestHelper.generateCode(model);
        
        var classes = generatorTestHelper.compileToClasses(code);        
        
        var myFunc = functionGeneratorHelper.createFunc(classes, "MyFunc");

        var result = functionGeneratorHelper.invokeFunc(myFunc, FieldWithMeta.class);
        
        var expected = generatorTestHelper.createInstanceUsingBuilder(classes, new RosettaJavaPackages.RootPackage("com.rosetta.test.model.metafields"), "ReferenceWithMetaFoo", Map.of(
                "value", generatorTestHelper.createInstanceUsingBuilder(classes, new RosettaJavaPackages.RootPackage("com.rosetta.test.model"), "Foo", Map.of(
                			"field", "someValue"
                		)),
                "reference", Reference.builder().setReference("someLocation")
        ));

        assertEquals(expected, result);    
    }  
    
    @Test
    void canSetAddressOnFunctionBasicOutput() {
        var model = """
        metaType address string

        func MyFunc:
            output:
                result string (1..1)
                    [metadata address]
            set result:  "someValue"
            set result -> address: "someLocation"
       """;

        var code = generatorTestHelper.generateCode(model);
        
        var classes = generatorTestHelper.compileToClasses(code);
        
        
        var myFunc = functionGeneratorHelper.createFunc(classes, "MyFunc");

        var result = functionGeneratorHelper.invokeFunc(myFunc, ReferenceWithMeta.class);

        var expected = generatorTestHelper.createInstanceUsingBuilder(classes, new RosettaJavaPackages.RootPackage("com.rosetta.model.metafields"), "ReferenceWithMetaString", Map.of(
                "value", "someValue",
                "reference", Reference.builder().setReference("someLocation")
        ));

        assertEquals(expected, result);
    }    
    
    
    @Test
    void canSetExternalIdOnFunctionBasicOutput() {
        var model = """
        metaType id string
        
        func MyFunc:
            inputs:
        		myValue string (1..1)
                myReference string (1..1)

            output:
                result string (1..1)
                [metadata id]
            set result: myValue
            set result -> id: myReference
        """;
        
        var code = generatorTestHelper.generateCode(model);
        var classes = generatorTestHelper.compileToClasses(code);         
        var myFunc = functionGeneratorHelper.createFunc(classes, "MyFunc");

        var result = functionGeneratorHelper.invokeFunc(myFunc, FieldWithMeta.class, "someValue", "someExternalKey");
        
        var expected = generatorTestHelper.createInstanceUsingBuilder(classes, new RosettaJavaPackages.RootPackage("com.rosetta.model.metafields"), "FieldWithMetaString", Map.of(
                "value", "someValue",
                "meta", MetaFields.builder().setExternalKey("someExternalKey")
        ));

        assertEquals(expected, result);
    }
   
    @Test
    void canSetExternalKeyOnFunctionObjectOutput() {
        var model = """
        metaType key string
        metaType reference string
        
        type Foo:
          [metadata key]
            a string (1..1)

        func MyFunc:
            inputs:
                myReference string (1..1)

            output:
                result Foo (1..1)
            set result -> a: "someA"
            set result -> key: myReference
        """;
        
      var code = generatorTestHelper.generateCode(model);
            
      var classes = generatorTestHelper.compileToClasses(code);
      var myFunc = functionGeneratorHelper.createFunc(classes, "MyFunc");
      
      var result = functionGeneratorHelper.invokeFunc(myFunc, RosettaModelObject.class, "someExternalReference");
      
      var expected = generatorTestHelper.createInstanceUsingBuilder(classes, new RosettaJavaPackages.RootPackage("com.rosetta.test.model"), "Foo", Map.of(
    			"a", "someA",
    			"meta", MetaFields.builder().setExternalKey("someExternalReference")
    		));
      
      assertEquals(expected, result);
    }
    
    @Test
    void canSetExternalReferenceOnFunctionObjectOutput() {
        var model = """
        metaType key string
        metaType reference string
        
        type Foo:
          [metadata key]
            a string (1..1)

        func MyFunc:
            inputs:
                myKey string (1..1)

            output:
                result Foo (1..1)
                  [metadata reference]
            set result -> reference: myKey
        """;
        
        var code = generatorTestHelper.generateCode(model);
                
        var classes = generatorTestHelper.compileToClasses(code);
        var myFunc = functionGeneratorHelper.createFunc(classes, "MyFunc");

        var result = functionGeneratorHelper.invokeFunc(myFunc, ReferenceWithMeta.class, "someExternalReference");
        
        var expected = generatorTestHelper.createInstanceUsingBuilder(classes, new RosettaJavaPackages.RootPackage("com.rosetta.test.model.metafields"), "ReferenceWithMetaFoo", Map.of(
                "externalReference", "someExternalReference"
        ));

        assertEquals(expected, result);
    }

    @Test
    void canSetMetaOnFunctionObjectOutputAndNestedBasicMetaField() {
        var model = """       
        type Foo:
            a string (1..1)
            b string (1..1)
                [metadata scheme]

        func MyFunc:
            output:
                result Foo (1..1)
                    [metadata scheme]
            set result -> scheme: "outerScheme"
            set result -> a:  "someValueA"
            set result -> b: "someValueB"
            set result -> b -> scheme: "innerScheme"
        """;

        var code = generatorTestHelper.generateCode(model);
        
        var classes = generatorTestHelper.compileToClasses(code);
        var myFunc = functionGeneratorHelper.createFunc(classes, "MyFunc");

        var result = functionGeneratorHelper.invokeFunc(myFunc, RosettaModelObject.class);

        var expected = generatorTestHelper.createInstanceUsingBuilder(classes, new RosettaJavaPackages.RootPackage("com.rosetta.test.model.metafields"), "FieldWithMetaFoo", Map.of(
                "value", generatorTestHelper.createInstanceUsingBuilder(classes, "Foo", Map.of(
                        "a", "someValueA",
                        "b", generatorTestHelper.createInstanceUsingBuilder(classes, new RosettaJavaPackages.RootPackage("com.rosetta.model.metafields"), "FieldWithMetaString", Map.of(
                                "value", "someValueB",
                                "meta", MetaFields.builder().setScheme("innerScheme")
                        ))
                )),
                "meta", MetaFields.builder().setScheme("outerScheme")
        ));

        assertEquals(expected, result);
    }

    @Test
    void canSetSchemeOnFunctionObjectOutput() {
        var model = """
        type Foo:
            a string (1..1)
            b string (1..1)

        func MyFunc:
            output:
                result Foo (1..1)
                    [metadata scheme]
            set result -> scheme: "outerScheme"
            set result -> a:  "someValueA"
            set result -> b: "someValueB"
        """;

        var code = generatorTestHelper.generateCode(model);
        var classes = generatorTestHelper.compileToClasses(code);
        var myFunc = functionGeneratorHelper.createFunc(classes, "MyFunc");

        var result = functionGeneratorHelper.invokeFunc(myFunc, RosettaModelObject.class);

        var expected = generatorTestHelper.createInstanceUsingBuilder(classes, new RosettaJavaPackages.RootPackage("com.rosetta.test.model.metafields"), "FieldWithMetaFoo", Map.of(
                "value", generatorTestHelper.createInstanceUsingBuilder(classes, "Foo", Map.of(
                        "a", "someValueA",
                        "b", "someValueB"
                )),
                "meta", MetaFields.builder().setScheme("outerScheme")
        ));

        assertEquals(expected, result);
    }

    @Test
    void canSetSchemeOnFunctionBasicOutput() {
        var model = """
        func MyFunc:
            output:
                result string (1..1)
                  [metadata scheme]
            set result:  "someValue"
            set result -> scheme: "someScheme"
        """;

        var code = generatorTestHelper.generateCode(model);
        var classes = generatorTestHelper.compileToClasses(code);

        var myFunc = functionGeneratorHelper.createFunc(classes, "MyFunc");

        var result =  functionGeneratorHelper.invokeFunc(myFunc, FieldWithMeta.class);

        var expected = generatorTestHelper.createFieldWithMetaString(classes, "someValue", "someScheme");

        assertEquals(expected, result);
    }
}
