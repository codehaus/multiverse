package org.multiverse.stms.alpha.instrumentation.fieldaccess;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalObject;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaTransactionalObject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.stms.alpha.instrumentation.AlphaReflectionUtils.existsField;

/**
 * A unit tests that checks if all fields/methods/interfaces of the FastAtomicObject are copied when the code is
 * instrumented (each atomicobject will receive the complete content of the fastatomicobject).
 * <p/>
 * This tests depends on the fact that the DefaultTxObjectMixin is used as donor. So when this is changed, this test
 * needs to be changed.
 *
 * @author Peter Veentjer.
 */
public class TransactionalMethod_DefaultTxObjectMixinTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
    }

    @Test
    public void test() {
        Class clazz = SomeTxObject.class;
        assertEquals(Object.class, clazz.getSuperclass());
        assertAllInstanceFieldsAreCopied(clazz);
        assertAllStaticFieldsAreCopied(clazz);
        assertAlphaAtomicObjectInterfaceIsCopied(clazz);
    }

    private void assertAllStaticFieldsAreCopied(Class clazz) {
        assertTrue(existsField(clazz, "___LOCKOWNER_UPDATER"));
        assertTrue(existsField(clazz, "___TRANLOCAL_UPDATER"));
        assertTrue(existsField(clazz, "___LISTENERS_UPDATER"));
    }

    private void assertAlphaAtomicObjectInterfaceIsCopied(Class clazz) {
        assertTrue(hasInterface(clazz, AlphaTransactionalObject.class));
    }

    private boolean hasInterface(Class clazz, Class theInterface) {
        for (Class anInterface : clazz.getInterfaces()) {
            if (anInterface.equals(theInterface)) {
                return true;
            }
        }

        return false;
    }

    private void assertAllInstanceFieldsAreCopied(Class clazz) {
        assertTrue(existsField(clazz, "___lockOwner"));
        assertTrue(existsField(clazz, "___tranlocal"));
        assertTrue(existsField(clazz, "___listeners"));
    }

    @TransactionalObject
    public static class SomeTxObject {

        int x;

        SomeTxObject(int x) {
            this.x = x;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }
    }
}
