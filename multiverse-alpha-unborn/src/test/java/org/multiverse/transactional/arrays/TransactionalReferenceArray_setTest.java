package org.multiverse.transactional.arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;

import static org.junit.Assert.*;
import static org.mockito.internal.util.StringJoiner.join;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class TransactionalReferenceArray_setTest {
    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void whenIndexTooBig_thenIndexOutOfBoundsException() {
        TransactionalReferenceArray<String> array = new TransactionalReferenceArray<String>(10);
        array.set(10, "foo");
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void whenIndexTooSmall_thenIndexOutOfBoundsException() {
        TransactionalReferenceArray<String> array = new TransactionalReferenceArray<String>(10);
        array.set(-1, "foo");
    }

    @Test
    public void whenNoTransactionAvailable_thenExecutedAtomically() {
        TransactionalReferenceArray<String> array = new TransactionalReferenceArray<String>(10);

        String update = "foo";
        long version = stm.getVersion();
        array.set(5, update);

        assertEquals(version + 1, stm.getVersion());
        assertSame(update, array.get(5));
    }

    @Test
    public void whenTransactionAvailable_thenThatTransactionIsUsed() {
        TransactionalReferenceArray<String> array = new TransactionalReferenceArray<String>(10);

        Transaction tx = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setReadonly(false)
                .build()
                .start();

        setThreadLocalTransaction(tx);
        long version = stm.getVersion();
        array.set(1, "foo");
        tx.abort();

        assertEquals(version, stm.getVersion());
        assertNull(array.get(1));
    }

    @Test
    public void whenNoChange() {
        TransactionalReferenceArray<String> array = new TransactionalReferenceArray<String>(10);

        long version = stm.getVersion();
        array.set(5, null);

        assertEquals(version, stm.getVersion());
        assertNull(array.atomicGet(5));
    }

    @Test
    public void whenListenersAvailable_thenTheyAreNotified() {
        TransactionalReferenceArray<Integer> array = new TransactionalReferenceArray<Integer>(10);
        WaitThread t = new WaitThread(0, array);
        startAll(t);

        sleepMs(500);
        assertAlive(t);

        //do the update
        array.set(3, 1);

        //wait for the waitthread to complete
        join(t);
    }

    class WaitThread extends TestThread {
        final TransactionalReferenceArray<Integer> array;

        WaitThread(int id, TransactionalReferenceArray<Integer> array) {
            super("WaitThread-" + id);
            this.array = array;
        }

        @Override
        @TransactionalMethod
        public void doRun() throws Exception {
            for (int k = 0; k < array.length(); k++) {
                if (array.get(k) != null) {
                    return;
                }
            }

            retry();
        }
    }
}
