package com.regnosys.rosetta.generator.java.function;

import com.regnosys.rosetta.tests.RosettaTestInjectorProvider;
import com.regnosys.rosetta.tests.util.CodeGeneratorTestHelper;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaTestInjectorProvider.class)

public class FunctionDistinctNumberTest {

    @Inject
    FunctionGeneratorHelper functionGeneratorHelper;
    @Inject
    CodeGeneratorTestHelper generatorTestHelper;

    BigDecimal toBigDecimal(Number n) {
        return switch (n.getClass().getSimpleName()) {
            case "Byte" -> BigDecimal.valueOf(n.byteValue());
            case "Double" -> BigDecimal.valueOf(n.doubleValue());
            case "Float" -> BigDecimal.valueOf(n.floatValue());
            case "Integer" -> BigDecimal.valueOf(n.intValue());
            case "Long" -> BigDecimal.valueOf(n.longValue());
            case "Short" -> BigDecimal.valueOf(n.shortValue());
            default -> BigDecimal.valueOf(n.doubleValue());
        };
    }

    @Test
    void javaStreamDistinctNumbers() {
        List<? extends Number> distinct = Stream.<Number>of(0d, 0, 0000f, 000000.00000, BigDecimal.ZERO, new BigDecimal("0000.0000"))
                .map(x -> toBigDecimal(x))
                .map(x -> x.stripTrailingZeros())
                .distinct()
                .toList();
        assertEquals(1, distinct.size());
    }

    @Test
    void distinctNumbers() {
        var model = """        
                func MyFunc:
                    output: result boolean (1..1)
                    set result: [ 0.0, 0.00 ] distinct count = 1
                """;

        var code = generatorTestHelper.generateCode(model);
        var classes = generatorTestHelper.compileToClasses(code);
        generatorTestHelper.writeClasses(code, "foo");
        var myFunc = functionGeneratorHelper.createFunc(classes, "MyFunc");
        var result = functionGeneratorHelper.invokeFunc(myFunc, Boolean.class);
        assertEquals(true, result);
    }

}
