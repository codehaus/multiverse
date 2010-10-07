package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.ReadWriteConflict;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

public abstract class BetaTransaction_ensureWritesTest implements BetaStmConstants {

    protected BetaStm stm;

    public abstract BetaTransaction newTransaction();

    public abstract BetaTransaction newTransaction(BetaTransactionConfiguration config);

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenEmpty() {
        BetaTransaction tx = newTransaction();

        tx.ensureWrites();

        assertIsActive(tx);
    }


    @Test
    public void whenContainsWrite() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = newTransaction();
        ref.incrementAndGet(tx, 1);
        tx.ensureWrites();

        assertIsActive(tx);
        assertHasUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertSame(tx, ref.___getLockOwner());
    }

    @Test
    public void whenContainsNonDirtyWrite() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = newTransaction();
        tx.openForWrite(ref, LOCKMODE_NONE);
        tx.ensureWrites();

        assertIsActive(tx);
        assertHasUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertSame(tx, ref.___getLockOwner());
    }

    @Test
    public void whenWriteConflicts() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = newTransaction();
        ref.incrementAndGet(tx, 1);

        ref.atomicIncrementAndGet(1);

        try {
            tx.ensureWrites();
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);
        assertHasNoUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertNull(ref.___getLockOwner());
    }

    @Test
    public void whenWriteEnsuredByOtherTransaction() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = newTransaction();
        ref.incrementAndGet(tx, 1);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.ensure(otherTx);

        try {
            tx.ensureWrites();
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);
        assertHasUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertSame(otherTx, ref.___getLockOwner());
    }

    @Test
    public void whenWritePrivatizedByOtherTransaction() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = newTransaction();
        ref.incrementAndGet(tx, 1);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.privatize(otherTx);

        try {
            tx.ensureWrites();
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);
        assertHasNoUpdateLock(ref);
        assertHasCommitLock(ref);
        assertSame(otherTx, ref.___getLockOwner());
    }

    @Test
    public void whenContainsRead() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = newTransaction();
        ref.get(tx);
        tx.ensureWrites();

        assertIsActive(tx);
        assertHasNoUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertNull(ref.___getLockOwner());
    }

    @Test
    public void whenContainsPrivatizedWrite() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = newTransaction();
        ref.incrementAndGet(tx, 1);
        ref.privatize(tx);
        tx.ensureWrites();

        assertIsActive(tx);
        assertHasNoUpdateLock(ref);
        assertHasCommitLock(ref);
        assertSame(tx, ref.___getLockOwner());
    }

    @Test
    public void whenContainsEnsuredWrite() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = newTransaction();
        ref.incrementAndGet(tx, 1);
        ref.ensure(tx);
        tx.ensureWrites();

        assertIsActive(tx);
        assertHasUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertSame(tx, ref.___getLockOwner());
    }

    @Test
    public void whenPrepared_thenPreparedTransactionException() {
        BetaTransaction tx = newTransaction();
        tx.prepare();

        try {
            tx.ensureWrites();
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        BetaTransaction tx = newTransaction();
        tx.commit();

        try {
            tx.ensureWrites();
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        BetaTransaction tx = newTransaction();
        tx.abort();

        try {
            tx.ensureWrites();
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
    }
}
