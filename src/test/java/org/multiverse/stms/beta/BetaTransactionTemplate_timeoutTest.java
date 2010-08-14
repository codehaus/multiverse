package org.multiverse.stms.beta;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.api.exceptions.RetryTimeoutException;
import org.multiverse.stms.beta.refs.LongRef;
import org.multiverse.stms.beta.refs.LongRefTranlocal;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.assertAlive;
import static org.multiverse.TestUtils.sleepMs;
import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.benchmarks.BenchmarkUtils.joinAll;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;
import static org.multiverse.stms.beta.ThreadLocalBetaObjectPool.getThreadLocalBetaObjectPool;

public class BetaTransactionTemplate_timeoutTest {

    private BetaStm stm;
    private BetaObjectPool pool;
    private LongRef ref;
    private long timeoutNs;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stm = new BetaStm();
        pool = new BetaObjectPool();
        ref = createLongRef(stm);
        timeoutNs = TimeUnit.SECONDS.toNanos(2);
    }

    @Test
    public void whenTimeout() {
        AtomicBlock block = stm.getTransactionFactoryBuilder()
                .setTimeoutNs(timeoutNs)
                .buildAtomicBlock();

        AwaitThread t = new AwaitThread(block);
        t.setPrintStackTrace(false);
        t.start();

        joinAll(t);
        t.assertFailedWithException(RetryTimeoutException.class);
        assertEquals(0, ref.unsafeLoad().value);
    }

    @Test
    public void whenSuccess() {
        AtomicBlock block = stm.getTransactionFactoryBuilder()
                .setTimeoutNs(timeoutNs)
                .buildAtomicBlock();

        AwaitThread t = new AwaitThread(block);
        t.setPrintStackTrace(false);
        t.start();

        sleepMs(500);
        assertAlive(t);

        stm.getDefaultAtomicBlock().execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                BetaTransaction btx = (BetaTransaction) tx;
                btx.openForWrite(ref, false, pool).value = 1;
            }
        });

        joinAll(t);
        t.assertNothingThrown();
        assertEquals(2, ref.unsafeLoad().value);
    }

    @Test
    public void whenNoWaitingNeededAndZeroTimeout() {
        stm.getDefaultAtomicBlock().execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                BetaTransaction btx = (BetaTransaction) tx;
                btx.openForWrite(ref, false, pool).value = 1;
            }
        });

        AtomicBlock block = stm.getTransactionFactoryBuilder()
                .setTimeoutNs(0)
                .buildAtomicBlock();

        AwaitThread t = new AwaitThread(block);
        t.setPrintStackTrace(false);
        t.start();

        joinAll(t);
        t.assertNothingThrown();
        assertEquals(2, ref.unsafeLoad().value);
    }

    class AwaitThread extends TestThread {

        private final AtomicBlock block;

        public AwaitThread(AtomicBlock block) {
            this.block = block;
        }

        @Override
        public void doRun() throws Exception {
            block.execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    BetaTransaction btx = (BetaTransaction) tx;
                    BetaObjectPool pool = getThreadLocalBetaObjectPool();

                    LongRefTranlocal write = btx.openForWrite(ref, false, pool);
                    if (write.value == 0) {
                        retry();
                    }

                    write.value = 2;
                }
            });
        }
    }
}
