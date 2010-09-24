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
public class FatArrayBetaTransaction_abortTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenUndefined() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.abort();

        assertIsAborted(tx);
    }

    @Test
    public void whenHasUnlockedWrite() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();
        int oldReadonlyCount = ref.___getReadonlyCount();

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.openForWrite(ref, LOCKMODE_NONE);

        tx.abort();

        assertIsAborted(tx);
        assertHasNoCommitLock(ref);
        assertSame(committed, ref.___unsafeLoad());
        assertSurplus(0, ref);
        assertNull(ref.___getLockOwner());
        assertReadonlyCount(oldReadonlyCount, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenHasLockedWrite() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();
        int oldReadonlyCount = ref.___getReadonlyCount();

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.openForWrite(ref, LOCKMODE_COMMIT);

        tx.abort();

        assertIsAborted(tx);
        assertHasNoCommitLock(ref);
        assertSame(committed, ref.___unsafeLoad());
        assertSurplus(0, ref);
        assertNull(ref.___getLockOwner());
        assertReadonlyCount(oldReadonlyCount, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenHasLockedRead() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();
        int oldReadonlyCount = ref.___getReadonlyCount();

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.openForRead(ref, LOCKMODE_COMMIT);

        tx.abort();

        assertIsAborted(tx);
        assertHasNoCommitLock(ref);
        assertSame(committed, ref.___unsafeLoad());
        assertSurplus(0, ref);
        assertNull(ref.___getLockOwner());
        assertReadonlyCount(oldReadonlyCount, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenHasPermanentReadThatIsNotLocked() {
        BetaLongRef ref = createReadBiasedLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();
        int oldReadonlyCount = ref.___getReadonlyCount();

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.openForRead(ref, LOCKMODE_NONE);

        tx.abort();

        assertIsAborted(tx);
        assertHasNoCommitLock(ref);
        assertReadBiased(ref);
        assertReadonlyCount(oldReadonlyCount, ref);
        assertSurplus(1, ref);
        assertSame(committed, ref.___unsafeLoad());
        assertNull(ref.___getLockOwner());
    }

    @Test
    public void whenHasConstructed() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
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
    @Ignore
    public void whenHasReads() {

    }

    @Test
    public void whenNormalListenerAvailable() {
        BetaLongRef ref = newLongRef(stm, 0);

        TransactionLifecycleListenerMock listenerMock = new TransactionLifecycleListenerMock();
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
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
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);
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
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.prepare();

        tx.abort();
        assertIsAborted(tx);
    }


    @Test
    public void whenAborted_thenIgnored() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.abort();

        tx.abort();
        assertIsAborted(tx);
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.commit();

        try {
            tx.abort();
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
    }
}
