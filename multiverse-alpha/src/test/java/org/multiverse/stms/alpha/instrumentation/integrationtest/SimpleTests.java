package org.multiverse.stms.alpha.instrumentation.integrationtest;

import org.junit.Test;
import org.multiverse.annotations.TransactionalObject;

/**
 * Created by IntelliJ IDEA. User: alarmnummer Date: Nov 12, 2009 Time: 2:23:54 PM To change this template use File |
 * Settings | File Templates.
 */
public class SimpleTests {

    @Test
    public void ref() {
        Ref1 ref1 = new Ref1();
        //ref1.set(10);
        //assertEquals(10, ref1.getClassMetadata());
    }

    @TransactionalObject
    static class Ref1 {
        int value;

        //public int getClassMetadata() {
        //    return value;
        //}

        //public void set(int value) {
        //    this.value = value;
        //}

        public void nothing() {
        }
    }
}
