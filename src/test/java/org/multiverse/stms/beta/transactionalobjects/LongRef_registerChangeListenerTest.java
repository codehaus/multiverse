package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.TestUtils;
import org.multiverse.api.blocking.CheapLatch;
import org.multiverse.api.blocking.Latch;
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
import static org.multiverse.TestUtils.getField;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;

public class LongRef_registerChangeListenerTest implements BetaStmConstants {
    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    public void whenCommuting() {
        BetaLongRef ref = createLongRef(stm, 0);

        LongFunction function = mock(LongFunction.class);
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.commute(ref, pool, function);

        Latch latch = new CheapLatch();
        long listenerEra = latch.getEra();
        LongRefTranlocal tranlocal = (LongRefTranlocal) tx.get(ref);
        int result = ref.___registerChangeListener(latch, tranlocal, pool, listenerEra);

        assertEquals(REGISTRATION_NONE, result);
        assertNull(getField(ref, "___listeners"));
        assertFalse(latch.isOpen());
        verifyZeroInteractions(function);
    }

    @Test
    public void whenInterestingWriteAlreadyHappened_thenLatchOpenedAndNoRegistration() {
        BetaLongRef ref = createLongRef(stm);

        BetaTransaction tx = stm.startDefaultTransaction();
        LongRefTranlocal read = tx.openForRead(ref, false, pool);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        LongRefTranlocal write = otherTx.openForWrite(ref, false, pool);
        write.value++;
        otherTx.commit(pool);

        Latch latch = new CheapLatch();
        long listenerEra = latch.getEra();
        int result = ref.___registerChangeListener(latch, read, pool, listenerEra);

        assertEquals(REGISTRATION_NOT_NEEDED, result);
        assertNull(getField(ref, "___listeners"));
        assertTrue(latch.isOpen());
    }

    @Test
    @Ignore
    public void whenLockedAndNoConflict_thenRegistered() {

    }

    @Test
    @Ignore
    public void whenLockedAndConflict() {

    }

    @Test
    public void whenConstructed_thenNoRegistration() {
        BetaTransaction tx = stm.startDefaultTransaction();
        BetaLongRef ref = new BetaLongRef(tx);
        LongRefTranlocal read = tx.openForConstruction(ref, pool);

        Latch latch = new CheapLatch();
        long listenerEra = latch.getEra();
        int result = ref.___registerChangeListener(latch, read, pool, listenerEra);

        assertEquals(REGISTRATION_NONE, result);
        TestUtils.assertHasNoListeners(ref);
        assertFalse(latch.isOpen());
    }

    @Test
    public void whenFirstOne_thenRegistrationSuccessful() {
        BetaLongRef ref = createLongRef(stm);

        BetaTransaction tx = stm.startDefaultTransaction();
        LongRefTranlocal read = tx.openForRead(ref, false, pool);

        Latch latch = new CheapLatch();
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
        BetaLongRef ref = createLongRef(stm);

        BetaTransaction tx1 = stm.startDefaultTransaction();
        LongRefTranlocal read1 = tx1.openForRead(ref, false, pool);

        Latch latch1 = new CheapLatch();
        long listenerEra1 = latch1.getEra();
        ref.___registerChangeListener(latch1, read1, pool, listenerEra1);

        BetaTransaction tx2 = stm.startDefaultTransaction();
        LongRefTranlocal read2 = tx2.openForRead(ref, false, pool);

        Latch latch2 = new CheapLatch();
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