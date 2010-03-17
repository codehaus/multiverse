package org.multiverse.stms.alpha.instrumentation.fieldaccess;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.alpha.AlphaStm;

import static org.junit.Assert.assertEquals;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;

/**
 * Tests if an atomic object is correctly transformed when a static initializer is used.
 * <p/>
 * There was an issue with the transformation of atomic methods containing assert statements, and to track down the
 * cause, this static initializer test was added because the assert statement relies on that behind the screens.
 * <p/>
 * The xause of the problem is that the static initializer of the FastAtomicMixin is added to the already existing
 * static initializer and this is not allowed.
 *
 * @author Peter Veentjer.
 */
public class TransactionalObject_StaticInitializerTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
    }

    @Test
    public void test() {
        TransactionalObject_StaticInitializerTest_SUT sut = new TransactionalObject_StaticInitializerTest_SUT();
        assertEquals(20, sut.getField());
    }
}
