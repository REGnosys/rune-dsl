package com.regnosys.rosetta.generator.java.condition;

import com.google.inject.Module;
import com.google.inject.util.Modules;
import com.regnosys.rosetta.config.file.RosettaConfigurationFileProvider;
import com.regnosys.rosetta.tests.RosettaTestInjectorProvider;
import com.regnosys.rosetta.tests.testmodel.JavaTestModel;
import com.regnosys.rosetta.tests.testmodel.RosettaTestModelService;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.path.RosettaPath;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.inject.Inject;

@ExtendWith(InjectionExtension.class)
@InjectWith(DataTypeConditionEmptyFalseTest.RosettaTestInjectorWithEmptyFalseEnabledProvider.class)
public class DataTypeConditionEmptyFalseTest extends AbstractConditionTest  {
    // Enable empty=false feature for this test
    public static class RosettaTestInjectorWithEmptyFalseEnabledProvider extends RosettaTestInjectorProvider {
        @Override
        protected com.google.inject.Module createRuntimeModule() {
            Module base = super.createRuntimeModule();
            return Modules.override(base).with(
                    binder -> binder.bind(RosettaConfigurationFileProvider.class)
                            .toInstance(RosettaConfigurationFileProvider.createFromClasspath("rosetta-config-with-enabled-empty-false.yml")));
        }
    }

    @Inject
    private RosettaTestModelService testModelService;

    @Test
    void thenExpressionWithAndThatResolvesFalseIsFailure() {
        JavaTestModel model = testModelService.toJavaTestModel("""
				type Bar:
				    barValue int (1..1)
				
				type Foo:
					fooValue int (0..1)
					bar Bar (0..1)
				
					condition C:
					    if fooValue exists
					    then fooValue > 0 and if bar exists
					        then bar -> barValue > 0
				""").compile();

        var condition = getCondition(model, "Foo", "C");

        RosettaModelObject foo = model.evaluateExpression(RosettaModelObject.class, """
				Foo {
				    fooValue: 10,
				    ...
				}
				""");

        var fooResults = condition.invoke(RosettaPath.valueOf("foo"), foo);

        assertResults(
                fooResults,
                (v1) -> assertFailure(v1, "FooC", "foo", "right of `and` operation is empty")
        );
    }
}
