package com.rosetta.model.lib.context.exampleargument;

import com.rosetta.model.lib.context.FunctionContextAccess;
import com.rosetta.model.lib.mapper.MapperS;

import javax.inject.Inject;

public class C {
    @Inject
    private FunctionContextAccess contextAccess;

    public int evaluate() {
        return contextAccess.evaluate(ctx -> {
           return MapperS.of(ctx.getArgument(MyContext.MyContextBuilder.class)).map("value", MyContext::getValue).getOrDefault(0);
        });
    }
}
