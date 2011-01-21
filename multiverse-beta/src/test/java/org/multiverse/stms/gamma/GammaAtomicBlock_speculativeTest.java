package org.multiverse.stms.gamma;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.stms.gamma.transactionalobjects.GammaLongRef;
import org.multiverse.stms.gamma.transactions.GammaTransaction;
import org.multiverse.stms.gamma.transactions.fat.FatLinkedGammaTransaction;
import org.multiverse.stms.gamma.transactions.fat.FatMapGammaTransaction;
import org.multiverse.stms.gamma.transactions.fat.FatMonoGammaTransaction;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.multiverse.TestUtils.assertInstanceof;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

public class GammaAtomicBlock_speculativeTest implements GammaConstants {

    private GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
    }

    @Test
    public void whenTransactionGrowing() {
        final GammaLongRef[] refs = new GammaLongRef[1000];
        for (int k = 0; k < refs.length; k++) {
            refs[k] = new GammaLongRef(stm);
        }

        final List<GammaTransaction> transactions = new LinkedList<GammaTransaction>();
        final AtomicInteger attempt = new AtomicInteger(1);

        AtomicBlock block = stm.newTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(true)
                .newAtomicBlock();

        block.execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                assertSame(tx, getThreadLocalTransaction());
                GammaTransaction btx = (GammaTransaction) tx;
                assertEquals(attempt.get(), tx.getAttempt());
                attempt.incrementAndGet();

                transactions.add(btx);
                for (GammaLongRef ref : refs) {
                    ref.openForWrite(btx, LOCKMODE_NONE).long_value = 1;
                }
            }
        });

        for (GammaLongRef ref : refs) {
            assertEquals(1, ref.atomicGet());
        }

        assertEquals(3, transactions.size());
        assertInstanceof(FatMonoGammaTransaction.class, transactions.get(0));
        assertInstanceof(FatLinkedGammaTransaction.class, transactions.get(1));
        assertInstanceof(FatMapGammaTransaction.class, transactions.get(2));
    }

    /*
    @Test
    public void whenPermanentListenerAdded() {
        final List<GammaTransaction> transactions = new LinkedList<GammaTransaction>();
        final AtomicBoolean added = new AtomicBoolean();
        final TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);

        AtomicBlock block = stm.createTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(true)
                .buildAtomicBlock();

        block.execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                GammaTransaction btx = (GammaTransaction) tx;
                transactions.add(btx);
                if (!added.get()) {
                    btx.registerPermanent( listener);
                }
            }
        });

        assertEquals(2, transactions.size());
        assertTrue(transactions.get(0) instanceof LeanMonoGammaTransaction);
        assertTrue(transactions.get(1) instanceof FatMonoGammaTransaction);
    }           */

    @Test
    @Ignore
    public void whenCommute() {
        /*
        final List<GammaTransaction> transactions = new LinkedList<GammaTransaction>();
        final GammaLongRef ref = new GammaLongRef(stm);
        final LongFunction function = mock(LongFunction.class);

        AtomicBlock block = stm.newTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(true)
                .buildAtomicBlock();

        block.execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                assertSame(tx, getThreadLocalTransaction());
                GammaTransaction btx = (GammaTransaction) tx;
                transactions.add(btx);
                btx.commute(ref, function);
            }
        });

        assertEquals(2, transactions.size());
        assertTrue(transactions.get(0) instanceof LeanMonoGammaTransaction);
        assertTrue(transactions.get(1) instanceof FatMonoGammaTransaction);
        */
    }

    @Test
    @Ignore
    public void whenNormalListenerAdded() {
        /*
        final List<GammaTransaction> transactions = new LinkedList<GammaTransaction>();
        final AtomicBoolean added = new AtomicBoolean();
        final TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);

        AtomicBlock block = stm.newTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(true)
                .buildAtomicBlock();

        block.execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                assertSame(tx, getThreadLocalTransaction());
                GammaTransaction btx = (GammaTransaction) tx;
                transactions.add(btx);

                if (!added.get()) {
                    btx.register(listener);
                }

            }
        });

        assertEquals(2, transactions.size());
        assertTrue(transactions.get(0) instanceof LeanMonoGammaTransaction);
        assertTrue(transactions.get(1) instanceof FatMonoGammaTransaction);    */
    }

    @Test
    @Ignore
    public void whenNormalListenersAvailable_thenTheyAreNotCopied() {

    }

    @Test
    public void whenTimeoutAvailable_thenCopied() {
        final GammaLongRef ref1 = new GammaLongRef(stm);
        final GammaLongRef ref2 = new GammaLongRef(stm);

        final List<GammaTransaction> transactions = new LinkedList<GammaTransaction>();

        AtomicBlock block = stm.newTransactionFactoryBuilder()
                .setTimeoutNs(1000)
                .setSpeculativeConfigurationEnabled(true)
                .newAtomicBlock();

        block.execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                assertSame(tx, getThreadLocalTransaction());
                GammaTransaction btx = (GammaTransaction) tx;
                transactions.add(btx);

                if (transactions.size() == 1) {
                    btx.remainingTimeoutNs = 500;
                } else {
                    assertEquals(500, btx.getRemainingTimeoutNs());
                }

                ref1.openForWrite(btx, LOCKMODE_NONE);
                ref2.openForWrite(btx, LOCKMODE_NONE);
            }
        });
    }
}
