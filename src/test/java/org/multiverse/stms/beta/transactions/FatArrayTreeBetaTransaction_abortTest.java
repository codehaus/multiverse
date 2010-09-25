package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.lifecycle.TransactionLifecycleEvent;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.LongRefTranlocal;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.beta.BetaStmUtils.newLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class FatArrayTreeBetaTransaction_abortTest {

    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenUnused() {
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.abort();

        assertIsAborted(tx);
    }

    @Test
    public void whenContainsReadBiasedRead() {
        BetaLongRef ref = createReadBiasedLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.openForRead(ref, LOCKMODE_NONE);
        tx.abort();

        assertIsAborted(tx);

        assertNull(ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
        assertHasNoCommitLock(ref.___getOrec());
        assertReadBiased(ref.___getOrec());
        assertReadonlyCount(0, ref.___getOrec());
        //once arrived, a depart will not be called on a readbiased tranlocal
        assertSurplus(1, ref.___getOrec());
    }

    @Test
    public void whenContainsNormalRead() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.openForRead(ref, LOCKMODE_NONE);
        tx.abort();

        assertIsAborted(tx);
        assertHasNoCommitLock(ref.___getOrec());
        assertUpdateBiased(ref);
        assertSurplus(0, ref);
        assertNull(ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
        assertFalse(committed.isPermanent);
    }
  
    @Test
    public void whenContainsUnlockedWrite() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.openForWrite(ref, LOCKMODE_NONE);
        tx.abort();

        assertIsAborted(tx);
        assertHasNoCommitLock(ref.___getOrec());
        assertUpdateBiased(ref);
        assertSurplus(0, ref);
        assertNull(ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
        assertFalse(committed.isPermanent);
    }

   @Test
    public void whenHasPrivatizedWrite() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();
        int oldReadonlyCount = ref.___getReadonlyCount();

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        ref.privatize(tx);

        tx.abort();

        assertIsAborted(tx);
        assertHasNoCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertNull(ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
        assertSurplus(0, ref);
        assertReadonlyCount(oldReadonlyCount, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenHasEnsuredWrite() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();
        int oldReadonlyCount = ref.___getReadonlyCount();

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        ref.ensure(tx);

        tx.abort();

        assertIsAborted(tx);
        assertHasNoCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertNull(ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
        assertSurplus(0, ref);
        assertReadonlyCount(oldReadonlyCount, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenHasPrivatizedRead() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();
        int oldReadonlyCount = ref.___getReadonlyCount();

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        ref.privatize(tx);

        tx.abort();

        assertIsAborted(tx);
        assertHasNoCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertNull(ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
        assertSurplus(0, ref);

        assertReadonlyCount(oldReadonlyCount, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenHasEnsuredRead() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();
        int oldReadonlyCount = ref.___getReadonlyCount();

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        ref.ensure(tx);

        tx.abort();

        assertIsAborted(tx);
        assertHasNoCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertNull(ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
        assertSurplus(0, ref);
        assertReadonlyCount(oldReadonlyCount, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenContainsConstructed_thenItemRemainsLocked() {
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        BetaLongRef ref = new BetaLongRef(tx);
        LongRefTranlocal write = tx.openForConstruction(ref);
        tx.abort();

        assertIsAborted(tx);

        assertSame(tx, ref.___getLockOwner());
        assertHasCommitLock(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertNull(ref.___unsafeLoad());
        assertFalse(write.isPermanent);
        assertFalse(write.isCommitted);
    }

    @Test
    public void whenNormalListenerAvailable() {
        BetaLongRef ref = newLongRef(stm, 0);

        TransactionLifecycleListenerMock listenerMock = new TransactionLifecycleListenerMock();
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.register(listenerMock);
        tx.openForWrite(ref, LOCKMODE_NONE);
        tx.abort();

        assertEquals(1, listenerMock.events.size());
        assertEquals(TransactionLifecycleEvent.PostAbort, listenerMock.events.get(0));
    }

    @Test
    public void whenPermanentListenerAvailable() {
        BetaLongRef ref = newLongRef(stm, 0);

        TransactionLifecycleListenerMock listenerMock = new TransactionLifecycleListenerMock();
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .addPermanentListener(listenerMock);
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(config);
        tx.openForWrite(ref, LOCKMODE_NONE);
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
        tx.prepare();

        tx.abort();
        assertIsAborted(tx);
    }

    @Test
    @Ignore
    public void whenUndefined() {
    }

    @Test
    public void whenAborted_thenIgnored() {
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.abort();

        tx.abort();
        assertIsAborted(tx);
    }

    @Test
    public void whenCommitted_DeadTransactionException() {
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.commit();

        try {
            tx.abort();
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
    }
}
