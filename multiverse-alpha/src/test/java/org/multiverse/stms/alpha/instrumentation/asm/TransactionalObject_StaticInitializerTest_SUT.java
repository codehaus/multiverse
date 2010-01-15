package org.multiverse.stms.alpha.instrumentation.asm;

import org.multiverse.annotations.TransactionalObject;

@TransactionalObject
public class TransactionalObject_StaticInitializerTest_SUT {

    public static int x = 10;

    static {
        x = 20;
    }

    private int field;

    public TransactionalObject_StaticInitializerTest_SUT() {
        field = x;
    }

    public int getField() {
        return field;
    }
}
