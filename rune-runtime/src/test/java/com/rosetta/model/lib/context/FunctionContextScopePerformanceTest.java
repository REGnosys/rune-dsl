package com.rosetta.model.lib.context;

import com.google.inject.Guice;
import com.rosetta.model.lib.context.examplescope.One;
import com.rosetta.model.lib.context.examplescope.ScopeA;
import com.rosetta.model.lib.context.examplescope.ScopeB;
import com.rosetta.model.lib.context.examplescope.Two;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Performance tests to verify the O(1) optimization of getOverride.
 * <p>
 * These tests automatically run with JIT disabled (-Xint) via separate Surefire execution
 * to measure realistic first-run performance. They run automatically during {@code mvn test}
 * or {@code mvn clean install}.
 */
@Tag("performance")
public class FunctionContextScopePerformanceTest {
    @Test
    void testPerformanceForADeepScopeStack() {
        // Test that deep scopes don't degrade performance significantly
        // This proves O(1) vs O(n) by comparing shallow vs deep stack performance
        // Tests first-run performance (realistic usage without JIT warmup)

        final int ITERATIONS = 10000;
        final int SHALLOW_DEPTH = 10;
        final int DEEP_DEPTH = 1000000;

        // Shallow stack
        FunctionContext shallowContext = createContextWithDepth(SHALLOW_DEPTH);

        // Deep stack
        FunctionContext deepContext = createContextWithDepth(DEEP_DEPTH);

        // Measure first-run performance (no JIT warmup - simulates real usage)
        long shallowMs = runBenchmark(shallowContext, ITERATIONS);
        long deepMs = runBenchmark(deepContext, ITERATIONS);

        System.out.println("Performance test (first-run, no JIT warmup):");
        System.out.println("  Shallow (depth " + SHALLOW_DEPTH + "): " + shallowMs + "ms for " + ITERATIONS + " lookups");
        System.out.println("  Deep (depth " + DEEP_DEPTH + "): " + deepMs + "ms for " + ITERATIONS + " lookups");
        double ratio = (double)deepMs / shallowMs;
        System.out.println("  Ratio: " + String.format("%.2f", ratio) + "x");

        // With O(1), ratio should be close to 1.0 even without JIT
        // With O(n), ratio would be ~10x (DEEP_DEPTH / SHALLOW_DEPTH)
        // Allow up to 3x difference for noise, cache effects, and first-run variance
        assert ratio < 3.0 : "Deep context is " + String.format("%.2f", ratio)
                            + "x slower than shallow - suggests O(n) behavior";
    }
    
    private FunctionContext createContextWithDepth(int depth) {
        FunctionContext context = FunctionContextImpl.create(Guice.createInjector());
        for (int i = 0; i < depth; i++) {
            context.pushScope(i % 2 == 0 ? ScopeA.class : ScopeB.class);
        }
        return context;
    }

    private long runBenchmark(FunctionContext context, int iterations) {
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            One one = context.getInstance(One.class);
            Two two = context.getInstance(Two.class);
            assertNotNull(one);
            assertNotNull(two);
        }
        return (System.nanoTime() - start) / 1_000_000;
    }
}
