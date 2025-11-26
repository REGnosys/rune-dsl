package com.rosetta.model.lib.context.examplescope;

import com.rosetta.model.lib.context.FunctionContextAccess;

import javax.inject.Inject;

public class TwoA extends Two {
    @Inject
    private FunctionContextAccess context;
    @Inject
    private Two superFunction;
    
    public int evaluate() {
        return context.evaluate(ctx -> {
            ctx.pushScope(ScopeA.class);
            return 2 * superFunction.evaluate();
        });
    }
}
