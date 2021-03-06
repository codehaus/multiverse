package org.multiverse.stms.alpha.instrumentation.fieldaccess;

import org.multiverse.annotations.TransactionalObject;

@TransactionalObject
public class ObjectWithStaticInitializer {

    public static int staticValue;

    static {
        staticValue = 100;
    }

    private int value;

    public ObjectWithStaticInitializer(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
