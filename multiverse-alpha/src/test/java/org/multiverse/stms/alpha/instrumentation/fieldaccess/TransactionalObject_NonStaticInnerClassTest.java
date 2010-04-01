package org.multiverse.stms.alpha.instrumentation.fieldaccess;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;
import org.multiverse.stms.alpha.AlphaStm;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.stms.alpha.instrumentation.AlphaReflectionUtils.*;

/**
 * @author Peter Veentjer
 */
public class TransactionalObject_NonStaticInnerClassTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
    }

    @Test
    public void nonStaticStructure() {
        assertFalse(existsField(NonStaticTxObject.class, "value"));

        assertTrue(existsTranlocalClass(NonStaticTxObject.class));
        assertTrue(existsTranlocalField(NonStaticTxObject.class, "value"));

        assertTrue(existsTranlocalSnapshotClass(NonStaticTxObject.class));
        assertTrue(existsTranlocalSnapshotField(NonStaticTxObject.class, "value"));
    }

    @Test
    public void nonStaticTxObjectUsage() {
        long version = stm.getVersion();

        NonStaticTxObject innerClass = new NonStaticTxObject(10);

        assertEquals(version, stm.getVersion());
        assertEquals(10, innerClass.getValue());
    }

    @TransactionalObject
    class NonStaticTxObject {

        int value;

        NonStaticTxObject(int value) {
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
        long version = stm.getVersion();

        Outer outer = new Outer();

        assertEquals(version, stm.getVersion());
        assertNull(outer.getInner());

        version = stm.getVersion();

        outer.newInner(10);

        assertEquals(version + 1, stm.getVersion());
        assertNotNull(outer.getInner());
        assertEquals(10, outer.getInner().getValue());
    }

    @TransactionalObject
    static class Outer {

        private Inner inner;

        public Outer() {
            inner = null;
        }

        @TransactionalMethod(readonly = true)
        public Inner getInner() {
            return inner;
        }

        public void newInner(int value) {
            inner = new Inner(value);
        }

        @TransactionalObject
        class Inner {

            private int value;

            Inner(int value) {
                this.value = value;
            }

            @TransactionalMethod(readonly = true)
            public int getValue() {
                return value;
            }
        }
    }
}
