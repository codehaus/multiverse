package org.multiverse.stms.beta;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.stms.beta.refs.LongRef;
import org.multiverse.stms.beta.refs.LongRefTranlocal;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.assertAlive;
import static org.multiverse.TestUtils.sleepMs;
import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.benchmarks.BenchmarkUtils.joinAll;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;

public class BetaTransactionTemplate_blockingTest {

    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    public void test() {
        final LongRef ref = createLongRef(stm);

        WaitThread t = new WaitThread(ref);
        t.start();

        sleepMs(1000);
        assertAlive(t);

        new BetaTransactionTemplate(stm){
            @Override
            public Object execute(BetaTransaction tx) throws Exception {
                LongRefTranlocal write = tx.openForWrite(ref, false, pool);
                write.value=1;
                return null;
            }
        }.execute(pool);

        joinAll(t);
        assertEquals(2, ref.unsafeLoad().value);
    }

    class WaitThread extends TestThread {
        final LongRef ref;

        public WaitThread(LongRef ref) {
            this.ref = ref;
        }

        @Override
        public void doRun() throws Exception {
            final BetaObjectPool pool = new BetaObjectPool();

            new BetaTransactionTemplate(stm) {
                @Override
                public Object execute(BetaTransaction tx) throws Exception {
                    LongRefTranlocal write = tx.openForWrite(ref, false, pool);
                    if (write.value == 0) {
                        retry();
                    }

                    write.value++;
                    return null;
                }
            }.execute(pool);
        }
    }
}
