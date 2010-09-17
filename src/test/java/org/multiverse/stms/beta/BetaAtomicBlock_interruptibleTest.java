package org.multiverse.stms.beta;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.api.exceptions.InvisibleCheckedException;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.LongRefTranlocal;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.assertAlive;
import static org.multiverse.TestUtils.sleepMs;
import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmUtils.newLongRef;

public class BetaAtomicBlock_interruptibleTest {

    private BetaStm stm;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stm = new BetaStm();
    }

    @Test
    public void whenNoTimeoutAndInterruptible() throws InterruptedException {
        final BetaLongRef ref = newLongRef(stm);

        AtomicBlock block = stm.createTransactionFactoryBuilder()
                .setInterruptible(true)
                .buildAtomicBlock();

        WaitWithoutTimeoutThread t = new WaitWithoutTimeoutThread(ref, block);
        t.setPrintStackTrace(false);
        t.start();

        sleepMs(1000);
        assertAlive(t);

        t.interrupt();

        t.join();

        t.assertFailedWithException(InvisibleCheckedException.class);
        assertEquals(0, ref.___unsafeLoad().value);
    }

    @Test
    public void whenTimeoutAndInterruptible() throws InterruptedException {
        final BetaLongRef ref = newLongRef(stm);

        AtomicBlock block = stm.createTransactionFactoryBuilder()
                .setTimeoutNs(TimeUnit.SECONDS.toNanos(10))
                .setInterruptible(true)
                .buildAtomicBlock();

        WaitWithoutTimeoutThread t = new WaitWithoutTimeoutThread(ref, block);
        t.setPrintStackTrace(false);
        t.start();

        sleepMs(1000);
        assertAlive(t);

        t.interrupt();

        t.join();

        t.assertFailedWithException(InvisibleCheckedException.class);
        assertEquals(0, ref.___unsafeLoad().value);
    }


    class WaitWithoutTimeoutThread extends TestThread {
        final BetaLongRef ref;
        private AtomicBlock block;

        public WaitWithoutTimeoutThread(BetaLongRef ref, AtomicBlock block) {
            this.ref = ref;
            this.block = block;
        }

        @Override
        public void doRun() throws Exception {
            block.execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    BetaTransaction btx = (BetaTransaction) tx;
                    LongRefTranlocal write = btx.openForWrite(ref, false);
                    if (write.value == 0) {
                        retry();
                    }

                    write.value = 100;
                }
            });
        }
    }
}
