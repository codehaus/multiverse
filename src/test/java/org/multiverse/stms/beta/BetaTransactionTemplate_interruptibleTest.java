package org.multiverse.stms.beta;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.stms.beta.refs.LongRef;
import org.multiverse.stms.beta.refs.LongRefTranlocal;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.assertAlive;
import static org.multiverse.TestUtils.sleepMs;
import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.benchmarks.BenchmarkUtils.joinAll;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;

public class BetaTransactionTemplate_interruptibleTest {

    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    public void whenNoTimeoutAndInterruptible() {
        final LongRef ref = createLongRef(stm);

        BetaTransactionFactory txFactory = stm.getTransactionFactoryBuilder()
                .setInterruptible(true)
                .build();

        WaitWithoutTimeoutThread t = new WaitWithoutTimeoutThread(ref, txFactory);
        t.setPrintStackTrace(false);
        t.start();

        sleepMs(1000);
        assertAlive(t);

        t.interrupt();

        joinAll(t);

        t.assertInterrupted();
        assertEquals(0, ref.unsafeLoad().value);
    }

    @Test
    public void whenTimeoutAndInterruptible() {
        final LongRef ref = createLongRef(stm);

        BetaTransactionFactory txFactory = stm.getTransactionFactoryBuilder()
                .setTimeoutNs(TimeUnit.SECONDS.toNanos(10))
                .setInterruptible(true)
                .build();

        WaitWithoutTimeoutThread t = new WaitWithoutTimeoutThread(ref, txFactory);
        t.setPrintStackTrace(false);
        t.start();

        sleepMs(1000);
        assertAlive(t);

        t.interrupt();

        joinAll(t);

        t.assertInterrupted();
        assertEquals(0, ref.unsafeLoad().value);
    }


    class WaitWithoutTimeoutThread extends TestThread {
        final LongRef ref;
        private BetaTransactionFactory transactionFactory;

        public WaitWithoutTimeoutThread(LongRef ref, BetaTransactionFactory transactionFactory) {
            this.ref = ref;
            this.transactionFactory = transactionFactory;
        }

        @Override
        public void doRun() throws Exception {
            final BetaObjectPool pool = new BetaObjectPool();

            new BetaTransactionTemplate(transactionFactory) {
                @Override
                public Object execute(BetaTransaction tx) throws Exception {
                    LongRefTranlocal write = tx.openForWrite(ref, false, pool);
                    if (write.value == 0) {
                        retry();
                    }

                    write.value = 100;
                    return null;
                }
            }.executeChecked(pool);
        }
    }


}
