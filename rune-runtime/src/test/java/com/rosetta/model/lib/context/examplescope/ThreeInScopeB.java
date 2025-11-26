package com.rosetta.model.lib.context.examplescope;

import com.rosetta.model.lib.context.ContextAwareProvider;
import com.rosetta.model.lib.context.FunctionContextAccess;

import javax.inject.Inject;

public class ThreeInScopeB {
    @Inject
    private FunctionContextAccess context;
    @Inject
    private ContextAwareProvider<ThreeInScopeA> threeInScopeAProvider;
    
    public int evaluate() {
        return context.evaluate(ctx -> {
            ctx.pushScope(ScopeB.class);
            ThreeInScopeA threeInScopeA = threeInScopeAProvider.get();
            return threeInScopeA.evaluate();
        });
    }
}
