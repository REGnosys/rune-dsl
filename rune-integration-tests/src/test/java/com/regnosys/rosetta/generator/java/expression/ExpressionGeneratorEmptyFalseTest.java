package com.regnosys.rosetta.generator.java.expression;

import com.google.inject.Module;
import com.google.inject.util.Modules;
import com.regnosys.rosetta.config.file.RosettaConfigurationFileProvider;
import com.regnosys.rosetta.generator.java.condition.DataTypeConditionEmptyFalseTest;
import com.regnosys.rosetta.generator.java.scoping.JavaIdentifierRepresentationService;
import com.regnosys.rosetta.generator.java.statement.JavaLocalVariableDeclarationStatement;
import com.regnosys.rosetta.generator.java.statement.JavaStatement;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.generator.java.util.ImportManagerExtension;
import com.regnosys.rosetta.rosetta.RosettaModel;
import com.regnosys.rosetta.rosetta.simple.Attribute;
import com.regnosys.rosetta.tests.RosettaTestInjectorProvider;
import com.regnosys.rosetta.tests.util.ExpressionParser;
import com.regnosys.rosetta.tests.util.ModelHelper;
import com.regnosys.rosetta.types.RObjectFactory;
import com.rosetta.util.DottedPath;
import com.rosetta.util.types.JavaType;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.xtend2.lib.StringConcatenationClient;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(InjectionExtension.class)
@InjectWith(DataTypeConditionEmptyFalseTest.RosettaTestInjectorWithEmptyFalseEnabledProvider.class)
public class ExpressionGeneratorEmptyFalseTest {
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
    private ExpressionGenerator expressionGenerator;

    @Inject
    private ExpressionParser expressionParser;

    @Inject
    private ImportManagerExtension importManagerExtension;

    @Inject
    private JavaTypeTranslator javaTypeTranslator;

    @Inject
    private RObjectFactory rObjectFactory;

    @Inject
    private ModelHelper modelHelper;

    @Inject
    private JavaDependencyProvider javaDependencyProvider;

    @Inject
    private JavaIdentifierRepresentationService javaIdentifierRepresentationService;

    @Inject
    private ExpressionScopeUtility scopeUtil;

    @Test
    public void testEvaluateEmptyIsFalse() {
        CharSequence expr = """
        empty or False
        """;

        String expected = """
        import com.rosetta.model.lib.expression.ComparisonResult;
        import com.rosetta.model.lib.mapper.MapperS;


        return ComparisonResult.ofEmpty().or(ComparisonResult.of(MapperS.of(false))).get();
        """;

        assertJavaCode(expected, expr, Boolean.class);
    }

    private void assertJavaCode(String expectedCode, CharSequence expr, Class<?> expectedType) {
        assertJavaCode(expectedCode, expr, expectedType, List.of(), List.of());
    }

    private void assertJavaCode(String expectedCode, CharSequence expr, Class<?> expectedType, List<RosettaModel> context, Collection<? extends CharSequence> attrs) {
        assertJavaCode(expectedCode, expr, JavaType.from(expectedType), context, attrs);
    }

    private void assertJavaCode(String expectedCode, CharSequence expr, JavaType expectedType, List<RosettaModel> context, Collection<? extends CharSequence> attrs) {
        Attribute[] attributes = attrs.stream()
                .map(a -> expressionParser.parseAttribute(a, context))
                .toArray(Attribute[]::new);

        var parsedExpr = expressionParser.parseExpression(expr, context, attributes);
        var dependencies = javaDependencyProvider.javaDependencies(parsedExpr);

        var pkg = DottedPath.of("test", "ns");
        var scope = scopeUtil.createTestExpressionScope(pkg);
        var fileScope = scope.getFileScope();

        List<StringConcatenationClient> dependencyStatements = dependencies.stream()
                .map(dep -> {
                    var identifier = scope.createIdentifier(javaIdentifierRepresentationService.toDependencyInstance(dep), StringUtils.uncapitalize(dep.getSimpleName()));
                    var declaration = new JavaLocalVariableDeclarationStatement(false, dep, identifier);
                    return new StringConcatenationClient() {
                        @Override
                        protected void appendTo(TargetStringConcatenation target) {
                            target.append("@");
                            target.append(Inject.class);
                            target.append(" ");
                            target.append(declaration);
                        }
                    };
                })
                .collect(Collectors.toList());

        List<JavaStatement> statements = new ArrayList<>();

        statements.addAll(
                Arrays.stream(attributes)
                        .map(attr -> rObjectFactory.buildRAttributeWithEnclosingType(null, attr))
                        .map(rAttr -> {
                            var metaType = javaTypeTranslator.toMetaJavaType(rAttr);
                            var identifier = scope.createIdentifier(rAttr, rAttr.getName());
                            return new JavaLocalVariableDeclarationStatement(false, metaType, identifier);
                        })
                        .toList()
        );
        var returnStmt = expressionGenerator.javaCode(parsedExpr, expectedType, scope).completeAsReturn();

        statements.add(returnStmt);

        var actualBody = statements.stream().reduce(JavaStatement::append).orElseThrow();

        var content = new StringConcatenationClient() {
            @Override
            protected void appendTo(TargetStringConcatenation sc) {
                for (int i = 0; i < dependencyStatements.size(); i++) {
                    sc.append(dependencyStatements.get(i));
                    if (i < dependencyStatements.size() - 1) {
                        sc.append("\n");
                    }
                }
                if (!dependencyStatements.isEmpty()) {
                    sc.append("\n\n");
                }
                actualBody.appendTo(sc);
            }
        };

        String actual = importManagerExtension
                .buildClass(pkg, content, fileScope)
                .replace("package test.ns;", "")
                .replace("\r", "")
                .replace("\t", "    ")
                .trim() + "\n";

        assertEquals(expectedCode, actual);
    }
}
