package org.multiverse.templates;

import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.multiverse.TestUtils.assertIsActive;
import static org.multiverse.api.GlobalStmInstance.setGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.LoadTooOldVersionException;
import org.multiverse.api.exceptions.ReadonlyException;
import org.multiverse.api.exceptions.TooManyRetriesException;
import org.multiverse.datastructures.refs.IntRef;
import org.multiverse.stms.alpha.AlphaStm;

import java.io.IOException;

public class AtomicTemplateTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = new AlphaStm();
        setGlobalStmInstance(stm);
        setThreadLocalTransaction(null);
    }

    @After
    public void tearDown() {
        setThreadLocalTransaction(null);
    }

    public Transaction startUpdateTransaction() {
        Transaction t = stm.startUpdateTransaction(null);
        setThreadLocalTransaction(t);
        return t;
    }

    @Test
    public void testEmptyTemplate() {
        long version = stm.getTime();

        new AtomicTemplate() {
            @Override
            public Object execute(Transaction t) throws Exception {
                return null;
            }
        }.execute();

        assertEquals(version, stm.getTime());
        assertNull(getThreadLocalTransaction());
    }

    @Test
    public void testSelfCreatedTransaction() {
        final IntRef value = new IntRef(0);

        long version = stm.getTime();

        new AtomicTemplate() {
            @Override
            public Object execute(Transaction t) throws Exception {
                value.inc();
                return null;
            }
        }.execute();

        assertEquals(version + 1, stm.getTime());
        assertNull(getThreadLocalTransaction());
        assertEquals(1, value.get());
    }

    @Test
    public void testLiftingOnExistingTransaction() {
        final IntRef value = new IntRef(0);

        Transaction t = startUpdateTransaction();

        long startVersion = stm.getTime();
        long startedCount = stm.getProfiler().sumKey1("updatetransaction.started.count");

        new AtomicTemplate() {
            @Override
            public Object execute(Transaction t) throws Exception {
                value.inc();
                return null;
            }
        }.execute();

        assertSame(t, getThreadLocalTransaction());
        assertIsActive(t);
        assertEquals(startVersion, stm.getTime());
        assertEquals(startedCount, stm.getProfiler().sumKey1("updatetransaction.started.count"));

        t.commit();
        assertEquals(startVersion + 1, stm.getTime());
        assertSame(t, getThreadLocalTransaction());
        setThreadLocalTransaction(null);
        assertEquals(1, value.get());
    }

    /**
     * Verifies that ignoring the thread-local transaction disables lifting.
     * <p/>
     * FIXME: Broken because value.inc() starts its *own* transaction which does not use the "ignore thread-local"
     * param. So you're actually getting a normal lifting transaction.
     */
    @Ignore("ignoreThreadLocalTransaction model is broken. See test comment.")
    @Test
    public void testIgnoringThreadLocalTransactionPreventsLifting() {
        final IntRef value = new IntRef(0);

        Transaction t = startUpdateTransaction();

        long startVersion = stm.getTime();
        long startedCount = stm.getProfiler().sumKey1("updatetransaction.started.count");

        new AtomicTemplate(stm, null, true, false, Integer.MAX_VALUE) {
            @Override
            public Object execute(Transaction t) throws Exception {
                value.inc();
                return null;
            }
        }.execute();

        // the thread-local transaction should *not* have been abortAndReturnRestarted
        assertSame(t, getThreadLocalTransaction());
        assertIsActive(t);

        // the AtomicTemplate should have committed its transaction
        setThreadLocalTransaction(null);
        assertEquals(1, value.get());
        setThreadLocalTransaction(t);

        // ignoring the thread local transaction disables lifting
        assertEquals(startVersion + 1, stm.getTime());
        assertEquals(startedCount + 1, stm.getProfiler().sumKey1("updatetransaction.started.count"));

        // committing the "top-level" transaction should not change the value
        t.commit();
        assertEquals(startVersion + 2, stm.getTime());
        assertSame(t, getThreadLocalTransaction());
        setThreadLocalTransaction(null);
        assertEquals(1, value.get());
    }

    @Test
    public void testExplicitAbort() {
        final IntRef value = new IntRef(0);

        long version = stm.getTime();

        try {
            new AtomicTemplate() {
                @Override
                public Object execute(Transaction t) throws Exception {
                    value.inc();
                    t.abort();
                    return null;
                }
            }.execute();

            fail();
        } catch (AbortedException ex) {
        }

        /*assertEquals(1, stm.getProfiler().sumKey1("updatetransaction.aborted.count"));*/
        assertNull(getThreadLocalTransaction());
        assertEquals(version, stm.getTime());
        assertEquals(0, value.get());
    }

    @Test
    public void testExplicitCommit() {
        final IntRef value = new IntRef(0);

        long version = stm.getTime();

        new AtomicTemplate() {
            @Override
            public Object execute(Transaction t) throws Exception {
                value.inc();
                t.commit();
                return null;
            }
        }.execute();

        assertEquals(version + 1, stm.getTime());
        assertNull(getThreadLocalTransaction());
        assertEquals(1, value.get());
    }

    @Test
    public void testRuntimeExceptionDoesNotCommitChanges() {
        final IntRef value = new IntRef(0);

        final Exception ex = new RuntimeException();

        try {
            new AtomicTemplate() {
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
        final IntRef value = new IntRef(0);

        final Exception ex = new IOException();

        try {
            new AtomicTemplate() {
                @Override
                public Object execute(Transaction t) throws Exception {
                    value.inc();
                    throw ex;
                }
            }.execute();
        } catch (AtomicTemplate.InvisibleCheckedException found) {
            assertSame(ex, found.getCause());
        }

        assertEquals(0, value.get());
    }

    @Test
    public void testRecursionDoesntCallProblems() {
        long version = stm.getTime();
        long startedCount = stm.getProfiler().sumKey1("updatetransaction.started.count");

        recursiveCall(100);

        assertEquals(version + 1, stm.getTime());
        assertEquals(startedCount + 1, stm.getProfiler().sumKey1("updatetransaction.started.count"));
    }


    public void recursiveCall(final int depth) {
        if (depth == 0) {
            new IntRef();
        } else {
            new AtomicTemplate() {
                @Override
                public Object execute(Transaction t) throws Exception {
                    recursiveCall(depth - 1);
                    return null;
                }
            }.execute();
        }
    }

    // =========  retry count ===============================

    @Test
    public void tooManyRetries() {
        int retryCount = 10;
        final IntRef ref = new IntRef();
        final IntHolder executeCounter = new IntHolder();

        long version = stm.getTime();

        try {
            new AtomicTemplate(stm, null, false, false, retryCount) {
                @Override
                public Object execute(Transaction t) throws Exception {
                    executeCounter.value++;
                    assertEquals(executeCounter.value, getAttemptCount());
                    ref.inc();

                    throw new LoadTooOldVersionException();
                }
            }.execute();

            fail();
        } catch (TooManyRetriesException expected) {
        }

        assertEquals(retryCount + 1, executeCounter.value);
        assertEquals(version, stm.getTime());
        assertEquals(0, ref.get());
    }

    private static class IntHolder {

        int value;
    }

    // =============== readonly support ===================

    @Test
    public void readonly() {
        final IntRef ref = new IntRef(0);

        long version = stm.getTime();

        try {
            new AtomicTemplate(stm, null, false, true, Integer.MAX_VALUE) {
                @Override
                public Object execute(Transaction t) throws Exception {
                    ref.inc();
                    return null;
                }
            }.execute();
        } catch (ReadonlyException ex) {
        }

        assertEquals(version, stm.getTime());
        assertEquals(0, ref.get());
    }
}
