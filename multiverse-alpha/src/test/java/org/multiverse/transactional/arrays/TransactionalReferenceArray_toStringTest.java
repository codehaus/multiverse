package org.multiverse.transactional.arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.alpha.AlphaStm;

import static org.junit.Assert.assertEquals;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class TransactionalReferenceArray_toStringTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @Test
    public void whenNoElements() {
        TransactionalReferenceArray array = new TransactionalReferenceArray(0);
        String result = array.toString();
        assertEquals("[]", result);
    }

    @Test
    public void whenSomeElements() {
        TransactionalReferenceArray array = new TransactionalReferenceArray(3);
        array.set(0, "zero");
        array.set(1, "one");
        array.set(2, "two");

        String result = array.toString();
        assertEquals("[zero, one, two]", result);
    }

    @Test
    public void whenNullElement() {
        TransactionalReferenceArray array = new TransactionalReferenceArray(1);

        String result = array.toString();
        assertEquals("[null]", result);
    }
}
