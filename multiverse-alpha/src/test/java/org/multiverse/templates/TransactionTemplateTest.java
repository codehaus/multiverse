package org.multiverse.templates;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.OldVersionNotFoundReadConflict;
import org.multiverse.api.exceptions.ReadonlyException;
import org.multiverse.api.exceptions.TooManyRetriesException;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.transactional.primitives.TransactionalInteger;

import java.io.IOException;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.assertIsActive;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.*;

public class TransactionTemplateTest {

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

    /**
     * Starts an update transaction and places it in the
     *
     * @return
     */
    public Transaction startThreadLocalUpdateTransaction() {
        Transaction t = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setReadonly(false)
                .build().start();
        setThreadLocalTransaction(t);
        return t;
    }

    @Test
    public void testEmptyTemplate() {
        long version = stm.getVersion();

        new TransactionTemplate() {
            @Override
            public Object execute(Transaction t) throws Exception {
                return null;
            }
        }.execute();

        assertEquals(version, stm.getVersion());
        assertNull(getThreadLocalTransaction());
    }

    @Test
    public void testSelfCreatedTransaction() {
        final TransactionalInteger value = new TransactionalInteger(0);

        long version = stm.getVersion();

        new TransactionTemplate() {
            @Override
            public Object execute(Transaction t) throws Exception {
                value.inc();
                return null;
            }
        }.execute();

        assertEquals(version + 1, stm.getVersion());
        assertNull(getThreadLocalTransaction());
        assertEquals(1, value.get());
    }

    @Test
    public void testLiftingOnExistingTransaction() {
        final TransactionalInteger value = new TransactionalInteger(0);

        Transaction t = startThreadLocalUpdateTransaction();

        long startVersion = stm.getVersion();

        new TransactionTemplate() {
            @Override
            public Object execute(Transaction t) throws Exception {
                value.inc();
                return null;
            }
        }.execute();

        assertSame(t, getThreadLocalTransaction());
        assertIsActive(t);
        assertEquals(startVersion, stm.getVersion());

        t.commit();
        assertEquals(startVersion + 1, stm.getVersion());
        assertSame(t, getThreadLocalTransaction());
        setThreadLocalTransaction(null);
        assertEquals(1, value.get());
    }

    @Test
    public void notThreadLocalAwarePreventsLifting() {
        TransactionFactory txFactory = stm.getTransactionFactoryBuilder()
                .setReadonly(false)
                .build();

        final Transaction outerTx = txFactory.start();
        setThreadLocalTransaction(outerTx);

        new TransactionTemplate(txFactory, false, true) {
            @Override
            public Object execute(Transaction innerTx) throws Exception {
                assertNotSame(innerTx, outerTx);
                return null;
            }
        }.execute();

        // the thread-local transaction should *not* have been abortAndReturnRestarted
        assertSame(outerTx, getThreadLocalTransaction());
        assertIsActive(outerTx);
    }

    @Test
    public void testExplicitAbort() {
        final TransactionalInteger value = new TransactionalInteger(0);

        long version = stm.getVersion();

        try {
            new TransactionTemplate() {
                @Override
                public Object execute(Transaction t) throws Exception {
                    value.inc();
                    t.abort();
                    return null;
                }
            }.execute();

            fail();
        } catch (DeadTransactionException ignore) {
        }

        /*assertEquals(1, stm.getProfiler().sumKey1("updatetransaction.aborted.count"));*/
        assertNull(getThreadLocalTransaction());
        assertEquals(version, stm.getVersion());
        assertEquals(0, value.get());
    }

    @Test
    public void testExplicitCommit() {
        final TransactionalInteger value = new TransactionalInteger(0);

        long version = stm.getVersion();

        new TransactionTemplate() {
            @Override
            public Object execute(Transaction t) throws Exception {
                value.inc();
                t.commit();
                return null;
            }
        }.execute();

        assertEquals(version + 1, stm.getVersion());
        assertNull(getThreadLocalTransaction());
        assertEquals(1, value.get());
    }

    @Test
    public void testRuntimeExceptionDoesNotCommitChanges() {
        final TransactionalInteger value = new TransactionalInteger(0);

        final Exception ex = new RuntimeException();

        try {
            new TransactionTemplate() {
                @Override
                public Object execute(Transaction t) throws Exception {
                    value.inc();
                    throw ex;
                }
            }.execute();
        } catch (Exception found) {
            assertSame(ex, found);
        }

        assertEquals(0, value.get());
    }

    @Test
    public void testCheckedException() {
        final TransactionalInteger value = new TransactionalInteger(0);

        final Exception ex = new IOException();

        try {
            new TransactionTemplate() {
                @Override
                public Object execute(Transaction t) throws Exception {
                    value.inc();
                    throw ex;
                }
            }.execute();
        } catch (TransactionTemplate.InvisibleCheckedException found) {
            assertSame(ex, found.getCause());
        }

        assertEquals(0, value.get());
    }

    @Test
    public void testRecursionDoesntCallProblems() {

        TransactionalInteger ref = new TransactionalInteger();

        long version = stm.getVersion();
        recursiveCall(100, ref, 10);

        assertEquals(version + 1, stm.getVersion());
        assertEquals(10, ref.get());
    }


    public void recursiveCall(final int depth, final TransactionalInteger ref, final int value) {
        if (depth == 0) {
            ref.set(value);
        } else {
            new TransactionTemplate() {
                @Override
                public Object execute(Transaction t) throws Exception {
                    recursiveCall(depth - 1, ref, value);
                    return null;
                }
            }.execute();
        }
    }

    // =========  retry count ===============================

    @Test
    public void tooManyRetries() {
        int retryCount = 10;

        TransactionFactory txFactory = stm.getTransactionFactoryBuilder()
                .setReadonly(false)
                .setMaxRetryCount(retryCount).build();


        final TransactionalInteger ref = new TransactionalInteger();
        final IntHolder executeCounter = new IntHolder();

        long version = stm.getVersion();

        try {
            new TransactionTemplate(txFactory) {
                @Override
                public Object execute(Transaction t) throws Exception {
                    executeCounter.value++;
                    ref.inc();
                    throw new OldVersionNotFoundReadConflict();
                }
            }.execute();

            fail();
        } catch (TooManyRetriesException expected) {
        }

        assertEquals(retryCount + 1, executeCounter.value);
        assertEquals(version, stm.getVersion());
        assertEquals(0, ref.get());
    }

    private static class IntHolder {

        int value;
    }

    // =============== readonly support ===================

    @Test
    public void whenReadonlyAndWrite_thenReadonlyException() {
        TransactionFactory txFactory = stm.getTransactionFactoryBuilder()
                .setReadonly(true).build();

        final TransactionalInteger ref = new TransactionalInteger(0);

        long version = stm.getVersion();

        try {
            new TransactionTemplate(txFactory) {
                @Override
                public Object execute(Transaction t) throws Exception {
                    ref.inc();
                    return null;
                }
            }.execute();
        } catch (ReadonlyException ex) {
        }

        assertEquals(version, stm.getVersion());
        assertEquals(0, ref.get());
    }
}
