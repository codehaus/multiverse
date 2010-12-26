package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.blocking.DefaultRetryLatch;
import org.multiverse.api.blocking.RetryLatch;
import org.multiverse.api.functions.LongFunction;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.Listeners;
import org.multiverse.stms.beta.transactions.BetaTransaction;
import org.multiverse.stms.beta.transactions.FatArrayTreeBetaTransaction;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmTestUtils.*;
import static org.multiverse.stms.beta.transactionalobjects.OrecTestUtils.assertSurplus;

public class VeryAbstractTransactionalObject_registerChangeListenerTest implements BetaStmConstants {
    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenCommuting() {
        BetaLongRef ref = newLongRef(stm, 0);

        LongFunction function = mock(LongFunction.class);
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.commute(ref, function);

        RetryLatch latch = new DefaultRetryLatch();
        long listenerEra = latch.getEra();
        BetaLongRefTranlocal tranlocal = (BetaLongRefTranlocal) tx.get(ref);
        int result = ref.___registerChangeListener(latch, tranlocal, pool, listenerEra);

        assertEquals(REGISTRATION_NONE, result);
        assertNull(getField(ref, "___listeners"));
        assertFalse(latch.isOpen());
        verifyZeroInteractions(function);
    }

    @Test
    public void whenInterestingWriteAlreadyHappened_thenLatchOpenedAndNoRegistration() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaLongRefTranlocal read = tx.openForRead(ref, LOCKMODE_NONE);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        BetaLongRefTranlocal write = otherTx.openForWrite(ref, LOCKMODE_NONE);
        write.value++;
        otherTx.commit();

        RetryLatch latch = new DefaultRetryLatch();
        long listenerEra = latch.getEra();
        int result = ref.___registerChangeListener(latch, read, pool, listenerEra);

        assertEquals(REGISTRATION_NOT_NEEDED, result);
        assertNull(getField(ref, "___listeners"));
        assertTrue(latch.isOpen());
    }

    @Test
    public void whenPrivatizedAndNoConflict_thenRegistered() {
        BetaLongRef ref = newLongRef(stm, 10);
        long version = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaLongRefTranlocal read = tx.openForRead(ref, LOCKMODE_NONE);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquireCommitLock(otherTx);

        RetryLatch latch = new DefaultRetryLatch();
        long listenerEra = latch.getEra();
        int result = ref.___registerChangeListener(latch, read, pool, listenerEra);

        assertEquals(REGISTRATION_DONE, result);
        assertSurplus(2, ref);
        assertHasListeners(ref, latch);
        assertRefHasCommitLock(ref, otherTx);
        assertVersionAndValue(ref, version, 10);
        assertFalse(latch.isOpen());
    }

    @Test
    public void whenEnsuredAndNoConflict_thenRegistered() {
        BetaLongRef ref = newLongRef(stm, 10);
        long version = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaLongRefTranlocal read = tx.openForRead(ref, LOCKMODE_NONE);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquireWriteLock(otherTx);

        RetryLatch latch = new DefaultRetryLatch();
        long listenerEra = latch.getEra();
        int result = ref.___registerChangeListener(latch, read, pool, listenerEra);

        assertEquals(REGISTRATION_DONE, result);
        assertSurplus(2, ref);
        assertHasListeners(ref, latch);
        assertRefHasWriteLock(ref, otherTx);
        assertVersionAndValue(ref, version, 10);
        assertFalse(latch.isOpen());
    }

    @Test
    public void whenPrivatizedAndInterestingChangeAlreadyHappened() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaLongRefTranlocal read = tx.openForRead(ref, LOCKMODE_NONE);

        ref.atomicIncrementAndGet(1);
        long version = ref.getVersion();

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquireCommitLock(otherTx);

        RetryLatch latch = new DefaultRetryLatch();
        long listenerEra = latch.getEra();
        int result = ref.___registerChangeListener(latch, read, pool, listenerEra);

        assertEquals(REGISTRATION_NOT_NEEDED, result);
        assertNull(getField(ref, "___listeners"));
        assertTrue(latch.isOpen());
        assertSurplus(2, ref);
        assertHasNoListeners(ref);
        assertRefHasCommitLock(ref, otherTx);
        assertVersionAndValue(ref, version, 1);
    }

    @Test
    public void wheEnsuredAndInterestingChangeAlreadyHappened() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaLongRefTranlocal read = tx.openForRead(ref, LOCKMODE_NONE);

        ref.atomicIncrementAndGet(1);
        long version = ref.getVersion();

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquireWriteLock(otherTx);

        RetryLatch latch = new DefaultRetryLatch();
        long listenerEra = latch.getEra();
        int result = ref.___registerChangeListener(latch, read, pool, listenerEra);

        assertEquals(REGISTRATION_NOT_NEEDED, result);
        assertNull(getField(ref, "___listeners"));
        assertTrue(latch.isOpen());
        assertSurplus(2, ref);
        assertHasNoListeners(ref);
        assertRefHasWriteLock(ref, otherTx);
        assertVersionAndValue(ref, version, 1);
    }

    @Test
    public void whenConstructed_thenNoRegistration() {
        BetaTransaction tx = stm.startDefaultTransaction();
        BetaLongRef ref = new BetaLongRef(tx);
        BetaLongRefTranlocal read = tx.openForConstruction(ref);

        RetryLatch latch = new DefaultRetryLatch();
        long listenerEra = latch.getEra();
        int result = ref.___registerChangeListener(latch, read, pool, listenerEra);

        assertEquals(REGISTRATION_NONE, result);
        assertHasNoListeners(ref);
        assertFalse(latch.isOpen());
    }

    @Test
    public void whenFirstOne_thenRegistrationSuccessful() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaLongRefTranlocal read = tx.openForRead(ref, LOCKMODE_NONE);

        RetryLatch latch = new DefaultRetryLatch();
        long listenerEra = latch.getEra();
        int result = ref.___registerChangeListener(latch, read, pool, listenerEra);

        assertEquals(REGISTRATION_DONE, result);
        Listeners listeners = (Listeners) getField(ref, "___listeners");
        assertNotNull(listeners);
        assertSame(latch, listeners.listener);
        assertNull(listeners.next);
        assertEquals(listenerEra, listeners.listenerEra);
        assertFalse(latch.isOpen());
    }

    @Test
    public void whenSecondOne_thenListenerAddedToChain() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx1 = stm.startDefaultTransaction();
        BetaLongRefTranlocal read1 = tx1.openForRead(ref, LOCKMODE_NONE);

        RetryLatch latch1 = new DefaultRetryLatch();
        long listenerEra1 = latch1.getEra();
        ref.___registerChangeListener(latch1, read1, pool, listenerEra1);

        BetaTransaction tx2 = stm.startDefaultTransaction();
        BetaLongRefTranlocal read2 = tx2.openForRead(ref, LOCKMODE_NONE);

        RetryLatch latch2 = new DefaultRetryLatch();
        long listenerEra2 = latch2.getEra();
        int result = ref.___registerChangeListener(latch2, read2, pool, listenerEra2);

        assertEquals(REGISTRATION_DONE, result);
        Listeners listeners = (Listeners) getField(ref, "___listeners");
        assertNotNull(listeners);
        assertSame(latch2, listeners.listener);
        assertEquals(listenerEra2, listeners.listenerEra);
        assertNotNull(listeners.next);
        assertSame(latch1, listeners.next.listener);
        assertEquals(listenerEra1, listeners.next.listenerEra);
        assertFalse(latch1.isOpen());
    }
}
