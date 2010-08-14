package org.multiverse.stms.beta;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.exceptions.RetryTimeoutException;
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

public class BetaTransactionTemplate_timeoutTest {

    private BetaStm stm;
    private BetaObjectPool pool;
    private LongRef ref;
    private long timeoutNs;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
        ref = createLongRef(stm);
        timeoutNs = TimeUnit.SECONDS.toNanos(2);
    }

    @Test
    public void whenTimeout() {
        BetaTransactionFactory factory = stm.getTransactionFactoryBuilder()
                .setTimeoutNs(timeoutNs)
                .build();

        AwaitThread t = new AwaitThread(factory);
        t.setPrintStackTrace(false);
        t.start();

        joinAll(t);
        t.assertFailedWithException(RetryTimeoutException.class);
        assertEquals(0, ref.unsafeLoad().value);
    }

    @Test
    public void whenSuccess() {
        BetaTransactionFactory factory = stm.getTransactionFactoryBuilder()
                .setTimeoutNs(timeoutNs)
                .build();

        AwaitThread t = new AwaitThread(factory);
        t.setPrintStackTrace(false);
        t.start();

        sleepMs(500);
        assertAlive(t);

        new BetaTransactionTemplate(stm) {
            @Override
            public Object execute(BetaTransaction tx) throws Exception {
                tx.openForWrite(ref, false, pool).value = 1;
                return null;
            }
        }.execute(pool);

        joinAll(t);
        t.assertNothingThrown();
        assertEquals(2, ref.unsafeLoad().value);
    }

    @Test
    public void whenNoWaitingNeededAndZeroTimeout() {
        new BetaTransactionTemplate(stm) {
            @Override
            public Object execute(BetaTransaction tx) throws Exception {
                tx.openForWrite(ref, false, pool).value = 1;
                return null;
            }
        }.execute(pool);

        BetaTransactionFactory factory = stm.getTransactionFactoryBuilder()
                .setTimeoutNs(0)
                .build();

        AwaitThread t = new AwaitThread(factory);
        t.setPrintStackTrace(false);
        t.start();

        joinAll(t);
        t.assertNothingThrown();
        assertEquals(2, ref.unsafeLoad().value);
    }

    class AwaitThread extends TestThread {

        private final BetaTransactionFactory txFactory;

        public AwaitThread(BetaTransactionFactory txFactory) {
            this.txFactory = txFactory;
        }

        @Override
        public void doRun() throws Exception {
            final BetaObjectPool pool = new BetaObjectPool();

            new BetaTransactionTemplate(txFactory) {
                @Override
                public Object execute(BetaTransaction tx) throws Exception {
                    LongRefTranlocal write = tx.openForWrite(ref, false, pool);
                    if (write.value == 0) {
                        retry();
                    }

                    write.value = 2;
                    return null;
                }
            }.execute(pool);
        }
    }
}
