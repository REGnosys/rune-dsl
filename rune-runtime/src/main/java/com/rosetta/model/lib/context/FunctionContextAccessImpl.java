package com.rosetta.model.lib.context;

import com.google.inject.Injector;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Implementation of {@link FunctionContextAccess} that maintains a stack of scopes with cached resolved overrides.
 * <p>
 * This implementation optimizes {@link #getInstance(Class)} to O(1) time complexity (with respect to the depth of the scope stack)
 * by maintaining a cache of resolved class overrides at each scope level. When a scope is entered, the cache is computed
 * by applying that scope's overrides to the parent scope's cache. When a scope is exited, the parent's
 * cache is automatically restored by popping the stack.
 */
@Singleton
public class FunctionContextAccessImpl implements FunctionContextAccess {
    private final ThreadLocal<FunctionContext> contextPerThread;
    private final Injector injector;
    
    @Inject
    public FunctionContextAccessImpl(Injector injector) {
        this.injector = injector;
        this.contextPerThread = ThreadLocal.withInitial(() -> FunctionContextImpl.create(injector));
    }

    @Override
    public void run(Consumer<FunctionContext> code) {
        FunctionContext parentContext = contextPerThread.get();
        FunctionContext context = parentContext.child();
        try {
            contextPerThread.set(context);
            code.accept(context);
        } finally {
            contextPerThread.set(parentContext);
        }
    }

    @Override
    public <T> T evaluate(Function<FunctionContext, T> code) {
        FunctionContext parentContext = contextPerThread.get();
        FunctionContext context = parentContext.child();
        try {
            contextPerThread.set(context);
            return code.apply(context);
        } finally {
            contextPerThread.set(parentContext);
        }
    }

    @Override
    public <T> T getInstanceInCurrentContext(Class<T> clazz) {
        return contextPerThread.get().getInstance(clazz);
    }

    @Override
    public void copyContextToCurrentThread(FunctionContext context) {
        contextPerThread.set(context.copy());
    }
}
