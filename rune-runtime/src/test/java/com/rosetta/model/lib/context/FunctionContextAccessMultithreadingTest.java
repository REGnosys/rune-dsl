package com.rosetta.model.lib.context;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.rosetta.model.lib.context.examplescope.One;
import com.rosetta.model.lib.context.examplescope.ScopeA;
import com.rosetta.model.lib.context.examplescope.ScopeB;
import com.rosetta.model.lib.context.examplescope.Two;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;

public class FunctionContextAccessMultithreadingTest {
    @Inject
    private FunctionContextAccess contextAccess;
    @Inject
    private ContextAwareProvider<One> oneProvider;
    @Inject
    private ContextAwareProvider<Two> twoProvider;
    
    @BeforeEach
    void setup() {
        Injector injector = Guice.createInjector();
        injector.injectMembers(this);
    }
    
    @Test
    void testStateIsCopiedCorrectlyToThreads() {
        contextAccess.run(ctx -> {
            ctx.pushScope(ScopeA.class);
            CompletableFuture<String> f1 = CompletableFuture.supplyAsync(() -> {
                contextAccess.copyContextToCurrentThread(ctx);
                return contextAccess.evaluate(ctx1 -> {
                    ctx1.pushScope(ScopeB.class);
                    return getImplementationName(oneProvider) + getImplementationName(twoProvider);
                });
            });
            CompletableFuture<String> f2 = CompletableFuture.supplyAsync(() -> {
                contextAccess.copyContextToCurrentThread(ctx);
                return getImplementationName(oneProvider) + getImplementationName(twoProvider);
            });
            CompletableFuture.allOf(f1, f2).join();
            
            String res1 = f1.join();
            String res2 = f2.join();
            String res3 = getImplementationName(oneProvider) + getImplementationName(twoProvider);

            Assertions.assertAll(
                    () -> Assertions.assertEquals("OneBTwoA", res1),
                    () -> Assertions.assertEquals("OneTwoA", res2),
                    () -> Assertions.assertEquals("OneTwoA", res3)
            );
        });
    }
    
    private String getImplementationName(ContextAwareProvider<?> provider) {
        return provider.get().getClass().getSimpleName();
    }
}
