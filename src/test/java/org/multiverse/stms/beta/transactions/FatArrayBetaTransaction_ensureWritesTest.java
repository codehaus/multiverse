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
import static org.multiverse.stms.beta.BetaStmUtils.newLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

public class FatArrayBetaTransaction_ensureWritesTest implements BetaStmConstants{
    
     private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenEmpty() {
        BetaTransaction tx = new FatArrayBetaTransaction(stm);

        tx.ensureWrites();

        assertIsActive(tx);
    }


    @Test
    public void whenContainsWrite() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = new FatArrayBetaTransaction(stm);
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

        BetaTransaction tx = new FatArrayBetaTransaction(stm);
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

        BetaTransaction tx = new FatArrayBetaTransaction(stm);
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

        BetaTransaction tx = new FatArrayBetaTransaction(stm);
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

        BetaTransaction tx = new FatArrayBetaTransaction(stm);
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

        BetaTransaction tx = new FatArrayBetaTransaction(stm);
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

        BetaTransaction tx = new FatArrayBetaTransaction(stm);
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

        BetaTransaction tx = new FatArrayBetaTransaction(stm);
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
        BetaTransaction tx = new FatArrayBetaTransaction(stm);
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
        BetaTransaction tx = new FatArrayBetaTransaction(stm);
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
        BetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.abort();

        try {
            tx.ensureWrites();
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
    }
}
