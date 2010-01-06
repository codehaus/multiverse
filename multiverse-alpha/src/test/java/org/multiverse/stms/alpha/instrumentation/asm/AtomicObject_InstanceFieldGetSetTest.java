package org.multiverse.stms.alpha.instrumentation.asm;

import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.multiverse.api.annotations.AtomicObject;

/**
 * @author Peter Veentjer
 */
public class AtomicObject_InstanceFieldGetSetTest {

    @After
    public void tearDown() {
        //assertNoInstrumentationProblems();
    }

    @Test
    public void simplePutAndGet() {
        PutOnField putOnField = new PutOnField();
        putOnField.doIt();
        assertEquals(1, putOnField.getValue());
    }

    @AtomicObject
    public static class PutOnField {
        int value;

        public void doIt() {
            value++;
        }

        public int getValue() {
            return value;
        }
    }

    @Test
    public void putAndGetOnReferencedNonAtomicObject() {
        NonAtomicObject nonAtomicObject = new NonAtomicObject();
        PutGetOnNonAtomic o = new PutGetOnNonAtomic(nonAtomicObject);
        o.doIt();
        assertEquals(10, o.getValue());
    }


    static class NonAtomicObject {
        int v;
    }

    @AtomicObject
    public static class PutGetOnNonAtomic {
        NonAtomicObject value;

        public PutGetOnNonAtomic(NonAtomicObject value) {
            this.value = new NonAtomicObject();
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
        SomeAtomicObject nonAtomicObject = new SomeAtomicObject();
        PutGetOnAtomic o = new PutGetOnAtomic(nonAtomicObject);
        o.doIt();
        assertEquals(10, o.getValue());
    }

    static class SomeAtomicObject {
        int v;
    }

    @AtomicObject
    public static class PutGetOnAtomic {
        SomeAtomicObject value;

        public PutGetOnAtomic(SomeAtomicObject value) {
            this.value = new SomeAtomicObject();
        }

        public void doIt() {
            value.v = 10;
        }

        public int getValue() {
            return value.v;
        }
    }
}
