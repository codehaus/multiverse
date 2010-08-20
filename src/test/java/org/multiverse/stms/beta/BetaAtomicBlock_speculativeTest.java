package org.multiverse.stms.beta;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.functions.LongFunction;
import org.multiverse.stms.beta.transactionalobjects.LongRef;
import org.multiverse.stms.beta.transactions.*;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;

public class BetaAtomicBlock_speculativeTest {

    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    public void whenTransactionGrowing() {
        final LongRef[] refs = new LongRef[1000];
        for (int k = 0; k < refs.length; k++) {
            refs[k] = createLongRef(stm);
        }

        final List<BetaTransaction> transactions = new LinkedList<BetaTransaction>();
        final AtomicInteger attempt = new AtomicInteger(1);

        AtomicBlock block = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigEnabled(true)
                .buildAtomicBlock();

        block.execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                BetaTransaction btx = (BetaTransaction) tx;
                assertEquals(attempt.get(), tx.getAttempt());
                attempt.incrementAndGet();

                transactions.add(btx);
                for (LongRef ref : refs) {
                    btx.openForWrite(ref, false, pool).value = 1;
                }
            }
        });

        for (LongRef ref : refs) {
            assertEquals(1, ref.___unsafeLoad().value);
        }

        assertEquals(3, transactions.size());
        assertTrue(transactions.get(0) instanceof LeanMonoBetaTransaction);
        assertTrue(transactions.get(1) instanceof LeanArrayBetaTransaction);
        assertTrue(transactions.get(2) instanceof LeanArrayTreeBetaTransaction);
    }

    /*
    @Test
    public void whenPermanentListenerAdded() {
        final List<BetaTransaction> transactions = new LinkedList<BetaTransaction>();
        final AtomicBoolean added = new AtomicBoolean();
        final TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);

        AtomicBlock block = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigEnabled(true)
                .buildAtomicBlock();

        block.execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                BetaTransaction btx = (BetaTransaction) tx;
                transactions.add(btx);
                if (!added.get()) {
                    btx.registerPermanent(pool, listener);
                }
            }
        });

        assertEquals(2, transactions.size());
        assertTrue(transactions.get(0) instanceof LeanMonoBetaTransaction);
        assertTrue(transactions.get(1) instanceof FatMonoBetaTransaction);
    }           */

    @Test
    public void whenCommute() {
        final List<BetaTransaction> transactions = new LinkedList<BetaTransaction>();
        final LongRef ref = createLongRef(stm);
        final LongFunction function = mock(LongFunction.class);

        AtomicBlock block = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigEnabled(true)
                .buildAtomicBlock();

        block.execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                BetaTransaction btx = (BetaTransaction) tx;
                transactions.add(btx);
                btx.commute(ref, pool, function);
            }
        });

        assertEquals(2, transactions.size());
        assertTrue(transactions.get(0) instanceof LeanMonoBetaTransaction);
        assertTrue(transactions.get(1) instanceof FatMonoBetaTransaction);
    }

    @Test
    public void whenNormalListenerAdded() {
        final List<BetaTransaction> transactions = new LinkedList<BetaTransaction>();
        final AtomicBoolean added = new AtomicBoolean();
        final TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);

        AtomicBlock block = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigEnabled(true)
                .buildAtomicBlock();

        block.execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                BetaTransaction btx = (BetaTransaction) tx;
                transactions.add(btx);

                if (!added.get()) {
                    btx.register(pool, listener);
                }

            }
        });

        assertEquals(2, transactions.size());
        assertTrue(transactions.get(0) instanceof LeanMonoBetaTransaction);
        assertTrue(transactions.get(1) instanceof FatMonoBetaTransaction);

    }

    @Test
    @Ignore
    public void whenNormalListenersAvailable_thenTheyAreNotCopied() {

    }

    @Test
    @Ignore
    public void whenTimeoutAvailable_thenCopied() {

    }
}
