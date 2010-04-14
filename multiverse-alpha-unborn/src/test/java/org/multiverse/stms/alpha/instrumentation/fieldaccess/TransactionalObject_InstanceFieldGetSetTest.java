package org.multiverse.stms.alpha.instrumentation.fieldaccess;

import org.junit.Test;
import org.multiverse.annotations.TransactionalObject;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Veentjer
 */
public class TransactionalObject_InstanceFieldGetSetTest {

    @Test
    public void simplePutAndGet() {
        PutOnField putOnField = new PutOnField();
        putOnField.doIt();
        assertEquals(1, putOnField.getValue());
    }

    @TransactionalObject
    public static class PutOnField {
        int value;

        public PutOnField() {
            value = 0;
        }

        public void doIt() {
            value++;
        }

        public int getValue() {
            return value;
        }
    }

    @Test
    public void putAndGetOnReferencedNonTxObject() {
        NonTxObject nonTxObject = new NonTxObject();
        PutGetOnNonTxObject o = new PutGetOnNonTxObject(nonTxObject);
        o.doIt();
        assertEquals(10, o.getValue());
    }


    static class NonTxObject {
        int v;
    }

    @TransactionalObject
    public static class PutGetOnNonTxObject {
        NonTxObject value;

        public PutGetOnNonTxObject(NonTxObject value) {
            this.value = new NonTxObject();
        }

        public void doIt() {
            value.v = 10;
        }

        public int getValue() {
            return value.v;
        }
    }

    @Test
    public void putAndGetOnReferencedAtomicObject() {
        SomeTxObject nonTxObject = new SomeTxObject();
        PutGetOnTxObject o = new PutGetOnTxObject(nonTxObject);
        o.doIt();
        assertEquals(10, o.getValue());
    }

    static class SomeTxObject {
        int v;
    }

    @TransactionalObject
    public static class PutGetOnTxObject {
        SomeTxObject value;

        public PutGetOnTxObject(SomeTxObject value) {
            this.value = new SomeTxObject();
        }

        public void doIt() {
            value.v = 10;
        }

        public int getValue() {
            return value.v;
        }
    }
}
