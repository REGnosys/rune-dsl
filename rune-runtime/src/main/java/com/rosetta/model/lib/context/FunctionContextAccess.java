package com.rosetta.model.lib.context;

import com.google.inject.ImplementedBy;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Provides a context for function execution with scoped overrides.
 */
@ImplementedBy(FunctionContextAccessImpl.class)
public interface FunctionContextAccess {
    /**
     * Executes code with access to the {@link FunctionContext}. Resets the context after execution.
     *
     * @param code the code to execute
     */
    void run(Consumer<FunctionContext> code);
    
    /**
     * Executes code with access to the {@link FunctionContext} and returns the result. Resets the context after execution.
     *
     * @param code the code to execute
     * @param <T> the return type
     * @return the result
     */
    <T> T evaluate(Function<FunctionContext, T> code);

    <T> T getInstanceInCurrentContext(Class<T> clazz);
    
    /**
     * Sets the current thread's scope state, typically after receiving it from another thread.
     * <p>
     * Use this method in a new thread to restore scope state that was captured
     * via {@link #copyStateOfCurrentThread()} in a parent thread.
     *
     * @param state the scope state to set for the current thread
     */
    void copyContextToCurrentThread(FunctionContext context);
}
