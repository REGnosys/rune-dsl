package com.regnosys.rosetta.generator.java.expression;

import com.google.inject.Module;
import com.google.inject.util.Modules;
import com.regnosys.rosetta.config.file.RosettaConfigurationFileProvider;
import com.regnosys.rosetta.generator.java.condition.DataTypeConditionEmptyFalseTest;
import com.regnosys.rosetta.tests.RosettaTestInjectorProvider;
import com.regnosys.rosetta.tests.testmodel.RosettaTestModelService;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith(InjectionExtension.class)
@InjectWith(DataTypeConditionEmptyFalseTest.RosettaTestInjectorWithEmptyFalseEnabledProvider.class)
public class EmptyEvaluationEmptyFalseTest {
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
    private RosettaTestModelService modelService;

    @Test
    void emptyEqualsEmptyEvaluatesToTrueTest() {
        var model = modelService.toJavaTestModel("").compile();

        boolean result = model.evaluateExpression(Boolean.class, "empty = empty");

        assertTrue(result);
    }

    @Test
    void emptyIfThenElseEvaluatesToNullTest() {
        var model = modelService.toJavaTestModel("").compile();

        Boolean result = model.evaluateExpression(Boolean.class, "if False then True");

        assertNull(result);
    }

    @Test
    void emptyEvaluatesToFalseTest() {
        var model = modelService.toJavaTestModel("").compile();

        boolean result = model.evaluateExpression(Boolean.class, "empty or False");

        assertFalse(result);
    }

    @Test
    void emptyInConstructorEvaluatesToFalseTest() {
        var model = modelService.toJavaTestModel("""
                type Foo:
                    someBoolean boolean (0..1)
                    alwaysFalse boolean (1..1)
                """).compile();

        boolean result = model.evaluateExpression(Boolean.class, "Foo { alwaysFalse: False, ... } -> someBoolean or Foo { alwaysFalse: False, ... } -> alwaysFalse");

        assertFalse(result);
    }
}
