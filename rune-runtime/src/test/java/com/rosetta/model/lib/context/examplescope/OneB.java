package com.rosetta.model.lib.context.examplescope;

import com.rosetta.model.lib.context.FunctionContextAccess;

import javax.inject.Inject;

public class OneB extends One {
    @Inject
    private FunctionContextAccess contextAccess;
    @Inject
    private One superFunction;

    public int evaluate() {
        return contextAccess.evaluate(ctx -> {
            ctx.pushScope(ScopeB.class);
            return 3 * superFunction.evaluate();
        });
    }
}
