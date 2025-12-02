package com.rosetta.model.lib.context.exampleargument;

import com.rosetta.model.lib.context.FunctionContextAccess;
import com.rosetta.model.lib.mapper.MapperS;

import javax.inject.Inject;

public class B {
    @Inject
    private FunctionContextAccess contextAccess;

    public int evaluate() {
        return contextAccess.evaluate(ctx -> {
            ctx.getArgument(MyContext.MyContextBuilder.class, MyContext.MyContextBuilder::new).setValue(2 * MapperS.of(ctx.getArgument(MyContext.MyContextBuilder.class)).map("value", MyContext::getValue).getOrDefault(0));
            int result = MapperS.of(ctx.getArgument(MyContext.MyContextBuilder.class)).map("value", MyContext::getValue).getOrDefault(0);
            return result;
        });
    }
}
