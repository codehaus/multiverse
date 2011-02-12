package org.multiverse.stms.gamma.transactions.fat;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.LockMode;
import org.multiverse.api.TransactionStatus;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.functions.LongFunction;
import org.multiverse.stms.gamma.GammaConstants;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.GammaLongRef;
import org.multiverse.stms.gamma.transactionalobjects.GammaRefTranlocal;
import org.multiverse.stms.gamma.transactions.GammaTransaction;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.stms.gamma.GammaTestUtils.*;

public abstract class FatGammaTransaction_abortTest<T extends GammaTransaction> implements GammaConstants {

    protected GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
    }

    protected abstract T newTransaction();

    protected abstract void assertCleaned(T tx);

    @Test
    public void whenUnused() {
        T tx = newTransaction();

        tx.abort();

        assertEquals(TransactionStatus.Aborted, tx.getStatus());
    }

    @Test
    public void locking_whenHasConstructed_thenRemainLocked() {
        GammaTransaction tx = newTransaction();
        GammaLongRef ref = new GammaLongRef(tx);
        GammaRefTranlocal write = tx.getRefTranlocal(ref);
        tx.abort();

        assertIsAborted(tx);

        assertLockMode(ref, LOCKMODE_EXCLUSIVE);
        assertSurplus(ref, 1);
        assertWriteBiased(ref);
        assertVersionAndValue(ref, 0, 0);
        assertFalse(write.hasDepartObligation());
        assertTrue(write.isConstructing());
    }

    @Test
    public void whenContainsCommutes() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        T tx = newTransaction();
        LongFunction function = mock(LongFunction.class);
        ref.commute(tx, function);
        GammaRefTranlocal tranlocal = tx.getRefTranlocal(ref);
        tx.abort();

        assertIsAborted(tx);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertNull(tranlocal.headCallable);
    }

    @Test
    public void whenHasRead() {
        whenHasRead(LockMode.None);
        whenHasRead(LockMode.Read);
        whenHasRead(LockMode.Write);
        whenHasRead(LockMode.Exclusive);
    }

    public void whenHasRead(LockMode readLockMode) {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        T tx = newTransaction();
        GammaRefTranlocal tranlocal = ref.openForRead(tx, readLockMode.asInt());
        tx.abort();

        assertIsAborted(tx);
        assertEquals(LOCKMODE_NONE, tranlocal.lockMode);
        assertNull(tranlocal.owner);
        assertEquals(initialValue, ref.long_value);
        assertEquals(initialVersion, ref.getVersion());
        assertCleaned(tx);
    }

    @Test
    public void whenHasWrite() {
        whenHasWrite(LockMode.None);
        whenHasWrite(LockMode.Read);
        whenHasWrite(LockMode.Write);
        whenHasWrite(LockMode.Exclusive);
    }

    public void whenHasWrite(LockMode writeLockMode) {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        T tx = newTransaction();
        GammaRefTranlocal tranlocal = ref.openForWrite(tx, writeLockMode.asInt());
        tx.abort();

        assertEquals(TransactionStatus.Aborted, tx.getStatus());
        assertEquals(LOCKMODE_NONE, tranlocal.lockMode);
        assertNull(tranlocal.owner);
        assertEquals(initialValue, ref.long_value);
        assertEquals(initialVersion, ref.getVersion());
        assertCleaned(tx);
    }

    @Test
    public void whenAborted() {
        T tx = newTransaction();
        tx.abort();

        tx.abort();

        assertEquals(TransactionStatus.Aborted, tx.getStatus());
        assertCleaned(tx);
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        T tx = newTransaction();
        tx.commit();

        try {
            tx.abort();
            fail();
        } catch (DeadTransactionException expected) {

        }

        assertEquals(TransactionStatus.Committed, tx.getStatus());
        assertCleaned(tx);
    }
}
