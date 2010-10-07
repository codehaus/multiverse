package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.exceptions.LockedException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.references.LongRef;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConfiguration;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.TestUtils.assertIsCommitted;
import static org.multiverse.api.ThreadLocalTransaction.*;
import static org.multiverse.stms.beta.BetaStmTestUtils.assertVersionAndValue;
import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

public class BetaLongRef_getAndIncrement1Test implements BetaStmConstants {

    private BetaStm stm;

    @Before
    public void setUp() {
        BetaStmConfiguration config = new BetaStmConfiguration();
        config.maxRetries = 10;
        stm = new BetaStm(config);
        clearThreadLocalTransaction();
    }

    @Test
    public void whenPreparedTransactionAvailable_thenPreparedTransactionException() {
        BetaLongRef ref = newLongRef(stm, 10);
        long version = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        tx.prepare();
        setThreadLocalTransaction(tx);

        try {
            ref.getAndIncrement(30);
            fail();
        } catch (PreparedTransactionException expected) {

        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, version, 10);
    }

    @Test
    public void whenActiveTransactionAvailable() {
        LongRef ref = newLongRef(stm, 10);

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        long value = ref.getAndIncrement(20);
        tx.commit();

        assertEquals(10, value);
        assertIsCommitted(tx);
        assertEquals(30, ref.atomicGet());
        assertSame(tx, getThreadLocalTransaction());
    }

    @Test
    public void whenNoChange() {
        BetaLongRef ref = newLongRef(stm, 10);
        long version = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        long value = ref.getAndIncrement(0);
        tx.commit();

        assertEquals(10, value);
        assertIsCommitted(tx);
        assertEquals(10, ref.atomicGet());
        assertSame(tx, getThreadLocalTransaction());
        assertVersionAndValue(ref, version, 10);
    }

    @Test
    @Ignore
    public void whenListenersAvailable() {

    }

    @Test
    public void whenLocked_thenLockedException() {
        BetaLongRef ref = newLongRef(stm, 10);
        long version = ref.getVersion();
        BetaTransaction tx = stm.startDefaultTransaction();
        tx.openForRead(ref, LOCKMODE_COMMIT);

        try {
            ref.getAndIncrement(1);
            fail();
        } catch (LockedException expected) {
        }

        assertSame(tx, ref.___getLockOwner());
        assertVersionAndValue(ref, version,10);
        assertSurplus(1, ref);
        assertHasCommitLock(ref);
    }

    @Test
    public void whenNoTransactionAvailable_thenExecutedAtomically() {
        BetaLongRef ref = newLongRef(stm, 10);

        long result = ref.getAndIncrement(1);

        assertSurplus(0, ref);
        assertHasNoCommitLock(ref);
        assertEquals(result, 10);
        assertEquals(11, ref.atomicGet());
        assertNull(getThreadLocalTransaction());
    }

    @Test
    public void whenCommittedTransactionAvailable() {
        BetaLongRef ref = newLongRef(stm, 10);
        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        tx.commit();

        long result = ref.getAndIncrement(10);

        assertSurplus(0, ref);
        assertHasNoCommitLock(ref);
        assertEquals(result, 10);
        assertEquals(20, ref.atomicGet());
        assertSame(tx, getThreadLocalTransaction());
        assertIsCommitted(tx);
    }

    @Test
    public void whenAbortedTransactionAvailable() {
        BetaLongRef ref = newLongRef(stm, 10);
        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        tx.abort();

        long result = ref.getAndIncrement(10);

        assertSurplus(0, ref);
        assertHasNoCommitLock(ref);
        assertEquals(result, 10);
        assertEquals(20, ref.atomicGet());
        assertSame(tx, getThreadLocalTransaction());
        assertIsAborted(tx);
    }

}
