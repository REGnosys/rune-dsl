package com.rosetta.model.lib.context.exampleargument;

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;

public class MyContext implements RosettaModelObject {
    protected int value;
    
    public static MyContextBuilder builder() {
        return new MyContextBuilder();
    }
    
    public MyContext(int value) {
        this.value = value;
    }
    
    public int getValue() {
        return value;
    }

    @Override
    public MyContextBuilder toBuilder() {
        return new MyContextBuilder().setValue(value);
    }

    @Override
    public MyContext build() {
        return this;
    }

    @Override
    public RosettaMetaData<? extends RosettaModelObject> metaData() {
        return null;
    }

    @Override
    public Class<? extends RosettaModelObject> getType() {
        return MyContext.class;
    }

    @Override
    public void process(RosettaPath path, Processor processor) {
        processor.processBasic(path, int.class, value, this);
    }

    public static class MyContextBuilder extends MyContext implements RosettaModelObjectBuilder {
        public MyContextBuilder() {
            super(0);
        }
        
        public MyContextBuilder setValue(int value) {
            this.value = value;
            return this;
        }

        @Override
        public MyContext build() {
            return new MyContext(value);
        }

        @Override
        public MyContextBuilder toBuilder() {
            return this;
        }

        @Override
        public <B extends RosettaModelObjectBuilder> B prune() {
            return null;
        }

        @Override
        public void process(RosettaPath path, BuilderProcessor processor) {
            processor.processBasic(path, int.class, value, this);
        }

        @Override
        public boolean hasData() {
            return true;
        }

        @Override
        public <B extends RosettaModelObjectBuilder> B merge(B other, BuilderMerger merger) {
            return null;
        }
    }
}
