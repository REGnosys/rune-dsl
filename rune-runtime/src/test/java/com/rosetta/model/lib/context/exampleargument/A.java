package com.rosetta.model.lib.context.exampleargument;

import com.rosetta.model.lib.context.ContextAwareProvider;
import com.rosetta.model.lib.context.FunctionContextAccess;

import javax.inject.Inject;

public class A {
    @Inject
    private FunctionContextAccess contextAccess;
    @Inject
    private ContextAwareProvider<B> bProvider;
    @Inject
    private ContextAwareProvider<C> cProvider;
    
    public int evaluate() {
        return contextAccess.evaluate(ctx -> {
            ctx.getArgument(MyContext.MyContextBuilder.class, MyContext::builder).setValue(1);
            B b = bProvider.get();
            C c = cProvider.get();
            int bResult = b.evaluate();
            int cResult = c.evaluate();
            return bResult + cResult;
        });
    }
}
