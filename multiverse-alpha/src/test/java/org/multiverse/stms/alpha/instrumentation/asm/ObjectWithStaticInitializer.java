package org.multiverse.stms.alpha.instrumentation.asm;

import org.multiverse.transactional.annotations.TransactionalObject;

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
