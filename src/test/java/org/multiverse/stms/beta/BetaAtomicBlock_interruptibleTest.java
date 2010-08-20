package org.multiverse.stms.beta;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.api.exceptions.InvisibleCheckedException;
import org.multiverse.stms.beta.transactionalobjects.LongRef;
import org.multiverse.stms.beta.transactionalobjects.LongRefTranlocal;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.assertAlive;
import static org.multiverse.TestUtils.sleepMs;
import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.benchmarks.BenchmarkUtils.joinAll;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;

public class BetaAtomicBlock_interruptibleTest {

    private BetaStm stm;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stm = new BetaStm();
    }

    @Test
    public void whenNoTimeoutAndInterruptible() {
        final LongRef ref = createLongRef(stm);

        AtomicBlock block = stm.getTransactionFactoryBuilder()
                .setInterruptible(true)
                .buildAtomicBlock();

        WaitWithoutTimeoutThread t = new WaitWithoutTimeoutThread(ref, block);
        t.setPrintStackTrace(false);
        t.start();

        sleepMs(1000);
        assertAlive(t);

        t.interrupt();

        joinAll(t);

        t.assertFailedWithException(InvisibleCheckedException.class);
        assertEquals(0, ref.___unsafeLoad().value);
    }

    @Test
    public void whenTimeoutAndInterruptible() {
        final LongRef ref = createLongRef(stm);

        AtomicBlock block = stm.getTransactionFactoryBuilder()
                .setTimeoutNs(TimeUnit.SECONDS.toNanos(10))
                .setInterruptible(true)
                .buildAtomicBlock();

        WaitWithoutTimeoutThread t = new WaitWithoutTimeoutThread(ref, block);
        t.setPrintStackTrace(false);
        t.start();

        sleepMs(1000);
        assertAlive(t);

        t.interrupt();

        joinAll(t);

        t.assertFailedWithException(InvisibleCheckedException.class);
        assertEquals(0, ref.___unsafeLoad().value);
    }


    class WaitWithoutTimeoutThread extends TestThread {
        final LongRef ref;
        private AtomicBlock block;

        public WaitWithoutTimeoutThread(LongRef ref, AtomicBlock block) {
            this.ref = ref;
            this.block = block;
        }

        @Override
        public void doRun() throws Exception {
            final BetaObjectPool pool = new BetaObjectPool();

            block.execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    BetaTransaction btx = (BetaTransaction) tx;
                    LongRefTranlocal write = btx.openForWrite(ref, false, pool);
                    if (write.value == 0) {
                        retry();
                    }

                    write.value = 100;
                }
            });
        }
    }
}