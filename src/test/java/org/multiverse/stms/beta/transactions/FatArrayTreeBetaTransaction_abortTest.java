package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.lifecycle.TransactionLifecycleEvent;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.LongRefTranlocal;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class FatArrayTreeBetaTransaction_abortTest {

    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    public void whenUnused() {
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.abort(pool);

        assertAborted(tx);
    }

    @Test
    public void whenContainsReadBiasedRead() {
        BetaLongRef ref = createReadBiasedLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.openForRead(ref, false, pool);
        tx.abort(pool);

        assertAborted(tx);

        assertNull(ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
        assertUnlocked(ref.___getOrec());
        assertReadBiased(ref.___getOrec());
        assertReadonlyCount(0, ref.___getOrec());
        //once arrived, a depart will not be called on a readbiased tranlocal
        assertSurplus(1, ref.___getOrec());
    }

    @Test
    public void whenContainsNormalRead() {
        BetaLongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.openForRead(ref, false, pool);
        tx.abort();

        assertAborted(tx);
        assertUnlocked(ref.___getOrec());
        assertUpdateBiased(ref);
        assertSurplus(0, ref);
        assertNull(ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
        assertFalse(committed.isPermanent);
    }

    @Test
    public void whenContainsLockedNormalRead() {
        BetaLongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.openForRead(ref, true, pool);
        tx.abort();

        assertAborted(tx);
        assertUnlocked(ref.___getOrec());
        assertUpdateBiased(ref);
        assertSurplus(0, ref);
        assertNull(ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
        assertFalse(committed.isPermanent);
    }

    @Test
    public void whenContainsUnlockedWrite() {
        BetaLongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.openForWrite(ref, false, pool);
        tx.abort();

        assertAborted(tx);
        assertUnlocked(ref.___getOrec());
        assertUpdateBiased(ref);
        assertSurplus(0, ref);
        assertNull(ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
        assertFalse(committed.isPermanent);
    }

    @Test
    public void whenContainsLockedWrite() {
        BetaLongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.openForWrite(ref, true, pool);
        tx.abort();

        assertAborted(tx);
        assertUnlocked(ref.___getOrec());
        assertUpdateBiased(ref);
        assertSurplus(0, ref);
        assertNull(ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
        assertFalse(committed.isPermanent);
    }

    @Test
    public void whenContainsConstructed_thenItemRemainsLocked() {
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        BetaLongRef ref = new BetaLongRef(tx);
        LongRefTranlocal write = tx.openForConstruction(ref, pool);
        tx.abort(pool);

        assertAborted(tx);

        assertSame(tx, ref.___getLockOwner());
        assertLocked(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertNull(ref.___unsafeLoad());
        assertFalse(write.isPermanent);
        assertFalse(write.isCommitted);
    }

    @Test
    public void whenNormalListenerAvailable() {
        BetaLongRef ref = createLongRef(stm, 0);

        TransactionLifecycleListenerMock listenerMock = new TransactionLifecycleListenerMock();
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.register(pool, listenerMock);
        tx.openForWrite(ref, false, pool);
        tx.abort();

        assertEquals(1, listenerMock.events.size());
        assertEquals(TransactionLifecycleEvent.PostAbort, listenerMock.events.get(0));
    }

    @Test
    public void whenPermanentListenerAvailable() {
        BetaLongRef ref = createLongRef(stm, 0);

        TransactionLifecycleListenerMock listenerMock = new TransactionLifecycleListenerMock();
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .addPermanentListener(listenerMock);
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(config);
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
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.prepare(pool);

        tx.abort(pool);
        assertAborted(tx);
    }

    @Test
    public void whenAborted_thenIgnored() {
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.abort(pool);

        tx.abort(pool);
        assertAborted(tx);
    }

    @Test
    public void whenCommitted_DeadTransactionException() {
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.commit(pool);

        try {
            tx.abort(pool);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertCommitted(tx);
    }
}
