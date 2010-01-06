package org.multiverse.stms.alpha.instrumentation.asm;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.api.GlobalStmInstance.setGlobalStmInstance;
import org.multiverse.api.annotations.AtomicMethod;
import org.multiverse.api.annotations.AtomicObject;
import org.multiverse.stms.alpha.AlphaStm;
import static org.multiverse.stms.alpha.instrumentation.AlphaReflectionUtils.*;

/**
 * @author Peter Veentjer
 */
public class AtomicObject_NonStaticInnerClassTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = new AlphaStm();
        setGlobalStmInstance(stm);
    }

    @Test
    public void nonStaticStructure() {
        assertFalse(existsField(NonStaticAtomicObject.class, "value"));

        assertTrue(existsTranlocalClass(NonStaticAtomicObject.class));
        assertTrue(existsTranlocalField(NonStaticAtomicObject.class, "value"));

        assertTrue(existsTranlocalSnapshotClass(NonStaticAtomicObject.class));
        assertTrue(existsTranlocalSnapshotField(NonStaticAtomicObject.class, "value"));
    }

    @Test
    public void nonStaticAtomicObjectUsage() {
        long version = stm.getTime();

        NonStaticAtomicObject innerClass = new NonStaticAtomicObject(10);

        assertEquals(version + 1, stm.getTime());
        assertEquals(10, innerClass.getValue());
    }

    @AtomicObject class NonStaticAtomicObject {

        int value;

        NonStaticAtomicObject(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }

    @Test
    public void outerWithInner() {
        long version = stm.getTime();

        Outer outer = new Outer();

        assertEquals(version + 1, stm.getTime());
        assertNull(outer.getInner());

        version = stm.getTime();

        outer.newInner(10);

        assertEquals(version + 1, stm.getTime());
        assertNotNull(outer.getInner());
        assertEquals(10, outer.getInner().getValue());
    }

    @AtomicObject
    static class Outer {

        private Inner inner;

        public Outer() {
            inner = null;
        }

        @AtomicMethod(readonly = true)
        public Inner getInner() {
            return inner;
        }

        public void newInner(int value) {
            inner = new Inner(value);
        }

        @AtomicObject class Inner {

            private int value;

            Inner(int value) {
                this.value = value;
            }

            @AtomicMethod(readonly = true)
            public int getValue() {
                return value;
            }
        }
    }
}
