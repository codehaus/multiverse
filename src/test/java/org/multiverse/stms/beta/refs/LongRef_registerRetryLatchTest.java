package org.multiverse.stms.beta.refs;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.blocking.CheapLatch;
import org.multiverse.api.blocking.Latch;
import org.multiverse.api.blocking.Listeners;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.ObjectPool;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.stms.beta.StmUtils.createLongRef;
import static org.multiverse.TestUtils.getField;

public class LongRef_registerRetryLatchTest {
    private BetaStm stm;
    private ObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new ObjectPool();
    }

    @Test
    public void whenInterestingWriteAlreadyHappened_thenLatchOpenedAndNoRegistration() {
        LongRef ref = createLongRef(stm);

        BetaTransaction tx = stm.start();
        LongRefTranlocal read = tx.openForRead(ref, false, pool);

        BetaTransaction otherTx = stm.start();
        LongRefTranlocal write = otherTx.openForWrite(ref, false, pool);
        write.value++;
        otherTx.commit(pool);

        Latch latch = new CheapLatch();
        long listenerEra = latch.getEra();
        boolean result = ref.registerChangeListener(latch, read, pool, listenerEra);

        assertFalse(result);
        assertNull(getField(ref, "listeners"));
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
    public void whenFirstOne_thenRegistrationSuccessful() {
        LongRef ref = createLongRef(stm);

        BetaTransaction tx = stm.start();
        LongRefTranlocal read = tx.openForRead(ref, false, pool);

        Latch latch = new CheapLatch();
        long listenerEra = latch.getEra();
        boolean result = ref.registerChangeListener(latch, read, pool, listenerEra);

        assertTrue(result);
        Listeners listeners = (Listeners) getField(ref, "listeners");
        assertNotNull(listeners);
        assertSame(latch, listeners.listener);
        assertNull(listeners.next);
        assertEquals(listenerEra, listeners.listenerEra);
        assertFalse(latch.isOpen());
    }

    @Test
    public void whenSecondOne_thenListenerAddedToChain() {
        LongRef ref = createLongRef(stm);

        BetaTransaction tx1 = stm.start();
        LongRefTranlocal read1 = tx1.openForRead(ref, false, pool);

        Latch latch1 = new CheapLatch();
        long listenerEra1 = latch1.getEra();
        ref.registerChangeListener(latch1, read1, pool, listenerEra1);

        BetaTransaction tx2 = stm.start();
        LongRefTranlocal read2 = tx2.openForRead(ref, false, pool);

        Latch latch2 = new CheapLatch();
        long listenerEra2 = latch2.getEra();
        boolean result = ref.registerChangeListener(latch2, read2, pool, listenerEra2);

        assertTrue(result);
        Listeners listeners = (Listeners) getField(ref, "listeners");
        assertNotNull(listeners);
        assertSame(latch2, listeners.listener);
        assertEquals(listenerEra2, listeners.listenerEra);
        assertNotNull(listeners.next);
        assertSame(latch1, listeners.next.listener);
        assertEquals(listenerEra1, listeners.next.listenerEra);
        assertFalse(latch1.isOpen());
    }
}