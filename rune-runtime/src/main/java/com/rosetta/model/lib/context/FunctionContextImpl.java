package com.rosetta.model.lib.context;

import com.google.inject.Injector;
import com.rosetta.model.lib.RosettaModelObjectBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class FunctionContextImpl implements FunctionContext {
    private final Injector injector;
    private final FunctionContextImpl parentContext;
    
    private List<FunctionScope> scopeStack = null;
    private Map<Class<?>, Class<?>> overridesCache = null;
    private Map<Class<? extends RosettaModelObjectBuilder>, RosettaModelObjectBuilder> arguments = null;

    public static FunctionContextImpl create(Injector injector) {
        return new FunctionContextImpl(injector, null);
    }
    
    private FunctionContextImpl(Injector injector, FunctionContextImpl parentContext) {
        this.injector = injector;
        this.parentContext = parentContext;
    }
    
    @Override
    public FunctionContextImpl child() {
        return new FunctionContextImpl(injector, this);
    }

    @Override
    public FunctionContextImpl copy() {
        FunctionContextImpl copy = new FunctionContextImpl(injector, parentContext == null ? null : parentContext.copy());
        copy.scopeStack = scopeStack == null ? null : new ArrayList<>(scopeStack);
        copy.overridesCache = overridesCache == null ? null : new HashMap<>(overridesCache);
        copy.arguments = arguments == null ? null : new HashMap<>(arguments);
        return copy;
    }

    @Override
    public void pushScope(Class<? extends FunctionScope> scopeClass) {
        FunctionScope scope = injector.getInstance(scopeClass);
        if (scopeStack == null) {
            scopeStack = new ArrayList<>();
            overridesCache = new HashMap<>();
        }
        scopeStack.add(scope);
        overridesCache.clear();
    }

    @Override
    public <T> T getInstance(Class<T> clazz) {
        Class<? extends T> resolvedClass = resolveOverride(clazz);
        return injector.getInstance(resolvedClass);
    }

    @SuppressWarnings("unchecked")
    private <T> Class<? extends T> resolveOverride(Class<T> clazz) {
        if (overridesCache == null) {
            return parentContext == null ? clazz : parentContext.resolveOverride(clazz);
        };
        return (Class<? extends T>) overridesCache.computeIfAbsent(clazz, key -> {
           Class<? extends T> result = parentContext == null ? clazz : parentContext.resolveOverride(clazz);
           for (FunctionScope scope : scopeStack) {
               result = scope.getOverride(result);
           }
           return result;
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends RosettaModelObjectBuilder> T getArgument(Class<T> clazz) {
        if (arguments != null) {
            T ownArgument = (T) arguments.get(clazz);
            if (ownArgument != null) {
                return ownArgument;
            }
        }
        if (parentContext == null) {
            return null;
        }
        T parentArgument = parentContext.getArgumentOrNull(clazz);
        if (parentArgument == null) {
            return null;
        }
        T initialValue = copy(parentArgument);
        checkType(clazz, initialValue);
        if (arguments == null) {
            arguments = new HashMap<>();
        }
        arguments.put(clazz, initialValue);
        return initialValue;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends RosettaModelObjectBuilder> T getArgument(Class<T> clazz, Supplier<T> initialValue) {
        if (arguments == null) {
            arguments = new HashMap<>();
        }
        return (T) arguments.computeIfAbsent(clazz, key -> {
            T initial = parentContext == null ? null : parentContext.getArgumentOrNull(clazz);
            if (initial == null) {
                initial = initialValue.get();
            } else {
                initial = copy(initial);
            }
            checkType(clazz, initial);
            return initial;
        });
    }
    @SuppressWarnings("unchecked")
    private <T extends RosettaModelObjectBuilder> T getArgumentOrNull(Class<T> clazz) {
        T ownArgument = arguments == null ? null : (T) arguments.get(clazz);
        if (ownArgument == null) {
            return parentContext == null ? null : parentContext.getArgumentOrNull(clazz);
        }
        return ownArgument;
    }

    @Override
    public <T extends RosettaModelObjectBuilder> void setArgument(Class<T> clazz, T value) {
        checkType(clazz, value);
        if (arguments == null) {
            arguments = new HashMap<>();
        }
        arguments.put(clazz, value);
    }

    @SuppressWarnings("unchecked")
    private <T extends RosettaModelObjectBuilder> T copy(T instance) {
        return (T) instance.build().toBuilder();
    }
    
    private <T> void checkType(Class<T> clazz, T arg) {
        if (!clazz.isInstance(arg)) {
            throw new IllegalArgumentException("Argument " + arg + " is not an instance of " + clazz);
        }
    }
}
