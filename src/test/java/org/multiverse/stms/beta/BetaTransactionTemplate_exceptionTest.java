package org.multiverse.stms.beta;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.InvisibleCheckedException;
import org.multiverse.stms.beta.refs.LongRef;
import org.multiverse.stms.beta.refs.LongRefTranlocal;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.multiverse.TestUtils.assertAborted;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;

public class BetaTransactionTemplate_exceptionTest {

    private BetaObjectPool pool;
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    public void whenCallingExecuteAndUncheckedExceptionThrown() {
        final LongRef ref = createLongRef(stm);
        final Exception ex = new RuntimeException();
        final List<BetaTransaction> transactions = new LinkedList<BetaTransaction>();

        try {
            new BetaTransactionTemplate(stm) {
                @Override
                public Object execute(BetaTransaction tx) throws Exception {
                    transactions.add(tx);
                    LongRefTranlocal write = tx.openForWrite(ref, false, pool);
                    write.value++;
                    throw ex;
                }
            }.execute(pool);
        } catch (Exception found) {
            assertSame(ex, found);
        }

        assertEquals(0, ref.unsafeLoad().value);
        assertEquals(1, transactions.size());
        assertAborted(transactions.get(0));
    }


    @Test
    public void whenCallingExecuteAndErrorThrown() {
        final LongRef ref = createLongRef(stm);
        final Error ex = new Error();
        final List<BetaTransaction> transactions = new LinkedList<BetaTransaction>();

        try {
            new BetaTransactionTemplate(stm) {
                @Override
                public Object execute(BetaTransaction tx) throws Exception {
                    transactions.add(tx);
                    LongRefTranlocal write = tx.openForWrite(ref, false, pool);
                    write.value++;
                    throw ex;
                }
            }.execute(pool);
        } catch (Error found) {
            assertSame(ex, found);
        }

        assertEquals(0, ref.unsafeLoad().value);
        assertEquals(1, transactions.size());
        assertAborted(transactions.get(0));
    }

    @Test
    public void whenCallingExecuteAndCheckedExceptionThrown() {
        final LongRef ref = createLongRef(stm);
        final Exception ex = new Exception();
        final List<BetaTransaction> transactions = new LinkedList<BetaTransaction>();

        try {
            new BetaTransactionTemplate(stm) {
                @Override
                public Object execute(BetaTransaction tx) throws Exception {
                    transactions.add(tx);
                    LongRefTranlocal write = tx.openForWrite(ref, false, pool);
                    write.value++;
                    throw ex;
                }
            }.execute(pool);
        } catch (InvisibleCheckedException found) {
            assertSame(ex, found.getCause());
        }

        assertEquals(0, ref.unsafeLoad().value);
        assertEquals(1, transactions.size());
        assertAborted(transactions.get(0));
    }

    @Test
    public void whenCallingExecuteCheckedAndUncheckedExceptionThrown() {
        final LongRef ref = createLongRef(stm);
        final Exception ex = new RuntimeException();
        final List<BetaTransaction> transactions = new LinkedList<BetaTransaction>();

        try {
            new BetaTransactionTemplate(stm) {
                @Override
                public Object execute(BetaTransaction tx) throws Exception {
                    transactions.add(tx);
                    LongRefTranlocal write = tx.openForWrite(ref, false, pool);
                    write.value++;
                    throw ex;
                }
            }.executeChecked(pool);
        } catch (Exception found) {
            assertSame(ex, found);
        }

        assertEquals(0, ref.unsafeLoad().value);
        assertEquals(1, transactions.size());
        assertAborted(transactions.get(0));
    }

    @Test
    public void whenCallingExecuteCheckedAndCheckedExceptionThrown() {
        final LongRef ref = createLongRef(stm);
        final Exception ex = new Exception();
        final List<BetaTransaction> transactions = new LinkedList<BetaTransaction>();

        try {
            new BetaTransactionTemplate(stm) {
                @Override
                public Object execute(BetaTransaction tx) throws Exception {
                    transactions.add(tx);
                    LongRefTranlocal write = tx.openForWrite(ref, false, pool);
                    write.value++;
                    throw ex;
                }
            }.executeChecked(pool);
        } catch (Exception found) {
            assertSame(ex, found);
        }

        assertEquals(0, ref.unsafeLoad().value);
        assertEquals(1, transactions.size());
        assertAborted(transactions.get(0));
    }

    @Test
    public void whenCallingExecuteCheckedAndErrorThrown() throws Exception {
        final LongRef ref = createLongRef(stm);
        final Error ex = new Error();
        final List<BetaTransaction> transactions = new LinkedList<BetaTransaction>();

        try {
            new BetaTransactionTemplate(stm) {
                @Override
                public Object execute(BetaTransaction tx) throws Exception {
                    transactions.add(tx);
                    LongRefTranlocal write = tx.openForWrite(ref, false, pool);
                    write.value++;
                    throw ex;
                }
            }.executeChecked(pool);
        } catch (Error found) {
            assertSame(ex, found);
        }

        assertEquals(0, ref.unsafeLoad().value);
        assertEquals(1, transactions.size());
        assertAborted(transactions.get(0));
    }
}
