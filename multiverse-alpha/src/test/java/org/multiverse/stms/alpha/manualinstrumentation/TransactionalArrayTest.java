package org.multiverse.stms.alpha.manualinstrumentation;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.GlobalStmInstance;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.templates.TransactionTemplate;

import static org.junit.Assert.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class TransactionalArrayTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = (AlphaStm) GlobalStmInstance.getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    @Test
    public void test() {
        TransactionalArray array = new TransactionalArray(10);
        assertEquals(10, array.length());
    }

    @Test
    public void set() {
        TransactionalArray<String> array = new TransactionalArray<String>(10);

        long version = stm.getVersion();

        array.set("foo", 5);
        array.set("bar", 6);

        assertEquals(version + 2, stm.getVersion());
        assertEquals("foo", array.get(5));
        assertEquals("bar", array.get(6));
    }

    @Test
    public void changesAreAtomic() {
        final TransactionalArray<String> array = new TransactionalArray<String>(10);
        //assertNull(array.get(0));
        //assertNull(array.get(1));

        try {
            new TransactionTemplate() {
                @Override
                public Object execute(Transaction tx) throws Exception {
                    array.set("one", 0);
                    array.set("two", 1);
                    tx.abort();
                    return null;
                }
            }.execute();
            fail();
        } catch (DeadTransactionException expected) {
        }

        clearThreadLocalTransaction();
        assertNull(array.get(0));
        assertNull(array.get(1));
    }
}
