package com.rosetta.model.lib.context;

import com.rosetta.model.lib.RosettaModelObjectBuilder;

import java.util.function.Supplier;

public interface FunctionContext {
    FunctionContext child();
    FunctionContext copy();
    void pushScope(Class<? extends FunctionScope> scopeClass);
    
    /**
     * Gets an instance of the specified class, applying any active scope overrides.
     *
     * @param clazz the class to instantiate
     * @param <T> the type
     * @return an instance of the class (or its override)
     */
    <T> T getInstance(Class<T> clazz);

    <T extends RosettaModelObjectBuilder> T getArgument(Class<T> clazz);
    <T extends RosettaModelObjectBuilder> T getArgument(Class<T> clazz, Supplier<T> initialValue);
    <T extends RosettaModelObjectBuilder> void setArgument(Class<T> clazz, T value);
}
