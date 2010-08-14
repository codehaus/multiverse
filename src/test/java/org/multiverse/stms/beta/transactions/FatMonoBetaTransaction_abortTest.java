package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.lifecycle.TransactionLifecycleEvent;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.refs.LongRef;
import org.multiverse.stms.beta.refs.LongRefTranlocal;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class FatMonoBetaTransaction_abortTest {
    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    public void whenUnused() {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.abort(pool);

        assertAborted(tx);
    }

    @Test
    public void whenContainsReadBiasedRead() {
        LongRef ref = createReadBiasedLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.openForRead(ref, false, pool);
        tx.abort(pool);

        assertAborted(tx);

        assertNull(ref.getLockOwner());
        assertSame(committed, ref.unsafeLoad());
        assertUnlocked(ref.getOrec());
        assertReadBiased(ref.getOrec());
        assertReadonlyCount(0, ref.getOrec());
        //once arrived, a depart will not be called on a readbiased tranlocal
        assertSurplus(1, ref.getOrec());
    }

    @Test
    public void whenContainsNormalRead() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.openForRead(ref, false, pool);
        tx.abort();

        assertAborted(tx);
        assertUnlocked(ref.getOrec());
        assertUpdateBiased(ref);
        assertSurplus(0, ref);
        assertNull(ref.getLockOwner());
        assertSame(committed, ref.unsafeLoad());
        assertFalse(committed.isPermanent);
    }

    @Test
    public void whenContainsLockedNormalRead() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.openForRead(ref, true, pool);
        tx.abort();

        assertAborted(tx);
        assertUnlocked(ref.getOrec());
        assertUpdateBiased(ref);
        assertSurplus(0, ref);
        assertNull(ref.getLockOwner());
        assertSame(committed, ref.unsafeLoad());
        assertFalse(committed.isPermanent);
    }

    @Test
    public void whenContainsUnlockedWrite() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.openForWrite(ref, false, pool);
        tx.abort();

        assertAborted(tx);
        assertUnlocked(ref.getOrec());
        assertUpdateBiased(ref);
        assertSurplus(0, ref);
        assertNull(ref.getLockOwner());
        assertSame(committed, ref.unsafeLoad());
        assertFalse(committed.isPermanent);
    }

    @Test
    public void whenContainsLockedWrite() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.openForWrite(ref, true, pool);
        tx.abort();

        assertAborted(tx);
        assertUnlocked(ref.getOrec());
        assertUpdateBiased(ref);
        assertSurplus(0, ref);
        assertNull(ref.getLockOwner());
        assertSame(committed, ref.unsafeLoad());
        assertFalse(committed.isPermanent);
    }

    @Test
    public void whenContainsConstructed_thenItemRemainsLocked() {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRef ref = new LongRef(tx);
        LongRefTranlocal write = tx.openForConstruction(ref, pool);
        tx.abort(pool);

        assertAborted(tx);

        assertSame(tx, ref.getLockOwner());
        assertLocked(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertNull(ref.unsafeLoad());
        assertFalse(write.isPermanent);
        assertFalse(write.isCommitted);
    }

    @Test
    public void whenNormalListenerAvailable() {
        LongRef ref = createLongRef(stm, 0);

        TransactionLifecycleListenerMock listenerMock = new TransactionLifecycleListenerMock();
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.register(pool, listenerMock);
        tx.openForWrite(ref, false, pool);
        tx.abort();

        assertEquals(1, listenerMock.events.size());
        assertEquals(TransactionLifecycleEvent.PostAbort, listenerMock.events.get(0));
    }

    @Test
    public void whenPermanentListenerAvailable() {
        LongRef ref = createLongRef(stm, 0);

        TransactionLifecycleListenerMock listenerMock = new TransactionLifecycleListenerMock();
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.registerPermanent(pool, listenerMock);
        tx.openForWrite(ref, false, pool);
        tx.abort();

        assertEquals(1, listenerMock.events.size());
        assertEquals(TransactionLifecycleEvent.PostAbort, listenerMock.events.get(0));
    }

    class TransactionLifecycleListenerMock implements TransactionLifecycleListener {
        List<Transaction> transactions = new LinkedList<Transaction>();
        List<TransactionLifecycleEvent> events = new LinkedList<TransactionLifecycleEvent>();

        @Override
        public void notify(Transaction transaction, TransactionLifecycleEvent e) {
            transactions.add(transaction);
            events.add(e);
        }
    }


    @Test
    public void whenPrepared() {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.prepare(pool);

        tx.abort(pool);
        assertAborted(tx);
    }

    @Test
    public void whenAborted_thenIgnored() {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.abort(pool);

        tx.abort(pool);
        assertAborted(tx);
    }

    @Test
    public void whenCommitted_DeadTransactionException() {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.commit(pool);

        try {
            tx.abort(pool);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertCommitted(tx);
    }
}
