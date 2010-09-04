package org.multiverse.stms.beta;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.api.functions.LongFunction;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
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

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenTransactionGrowing() {
        final BetaLongRef[] refs = new BetaLongRef[1000];
        for (int k = 0; k < refs.length; k++) {
            refs[k] = createLongRef(stm);
        }

        final List<BetaTransaction> transactions = new LinkedList<BetaTransaction>();
        final AtomicInteger attempt = new AtomicInteger(1);

        AtomicBlock block = stm.createTransactionFactoryBuilder()
                .setSpeculativeConfigEnabled(true)
                .buildAtomicBlock();

        block.execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                BetaTransaction btx = (BetaTransaction) tx;
                assertEquals(attempt.get(), tx.getAttempt());
                attempt.incrementAndGet();

                transactions.add(btx);
                for (BetaLongRef ref : refs) {
                    btx.openForWrite(ref, false).value = 1;
                }
            }
        });

        for (BetaLongRef ref : refs) {
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

        AtomicBlock block = stm.createTransactionFactoryBuilder()
                .setSpeculativeConfigEnabled(true)
                .buildAtomicBlock();

        block.execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                BetaTransaction btx = (BetaTransaction) tx;
                transactions.add(btx);
                if (!added.get()) {
                    btx.registerPermanent( listener);
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
        final BetaLongRef ref = createLongRef(stm);
        final LongFunction function = mock(LongFunction.class);

        AtomicBlock block = stm.createTransactionFactoryBuilder()
                .setSpeculativeConfigEnabled(true)
                .buildAtomicBlock();

        block.execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                BetaTransaction btx = (BetaTransaction) tx;
                transactions.add(btx);
                btx.commute(ref, function);
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

        AtomicBlock block = stm.createTransactionFactoryBuilder()
                .setSpeculativeConfigEnabled(true)
                .buildAtomicBlock();

        block.execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                BetaTransaction btx = (BetaTransaction) tx;
                transactions.add(btx);

                if (!added.get()) {
                    btx.register(listener);
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
    public void whenTimeoutAvailable_thenCopied() {
        final BetaLongRef ref1 = createLongRef(stm);
        final BetaLongRef ref2 = createLongRef(stm);

        final List<BetaTransaction> transactions = new LinkedList<BetaTransaction>();

        AtomicBlock block = stm.createTransactionFactoryBuilder()
                .setTimeoutNs(1000)
                .setSpeculativeConfigEnabled(true)
                .buildAtomicBlock();

        block.execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                BetaTransaction btx = (BetaTransaction) tx;
                transactions.add(btx);

                if(transactions.size()==1){
                    btx.setRemainingTimeoutNs(500);
                }else{
                    assertEquals(500, btx.getRemainingTimeoutNs());
                }

                btx.openForWrite(ref1,false);
                btx.openForWrite(ref2,false);
            }
        });
    }
}
