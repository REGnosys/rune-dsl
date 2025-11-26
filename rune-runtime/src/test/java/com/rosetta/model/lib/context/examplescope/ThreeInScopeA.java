package com.rosetta.model.lib.context.examplescope;

import com.rosetta.model.lib.context.ContextAwareProvider;
import com.rosetta.model.lib.context.FunctionContextAccess;

import javax.inject.Inject;

public class ThreeInScopeA {
    @Inject
    private FunctionContextAccess context;
    @Inject
    private ContextAwareProvider<Three> threeProvider;
    
    public int evaluate() {
        return context.evaluate(ctx -> {
            ctx.pushScope(ScopeA.class);
            Three three = threeProvider.get();
            return three.evaluate();
        });
    }
}
