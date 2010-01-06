package org.multiverse.stms.alpha.instrumentation.asm;

import org.multiverse.api.annotations.AtomicObject;

@AtomicObject
public class AtomicObject_StaticInitializerTest_SUT {

    public static int x = 10;

    static {
        x = 20;
    }

    private int field;

    public AtomicObject_StaticInitializerTest_SUT() {
        field = x;
    }

    public int getField() {
        return field;
    }
}
