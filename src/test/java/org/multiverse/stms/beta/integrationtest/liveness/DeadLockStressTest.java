package org.multiverse.stms.beta.integrationtest.liveness;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaTransactionTemplate;
import org.multiverse.stms.beta.refs.IntRef;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmUtils.createIntRef;
import static org.multiverse.stms.beta.ThreadLocalBetaObjectPool.getThreadLocalBetaObjectPool;

public class DeadLockStressTest {

    private volatile boolean stop;
    private int refCount = 100;
    private int threadCount = 10;
    private IntRef[] refs;
    private ChangeThread[] threads;
    private BetaStm stm;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stop = false;
        stm = new BetaStm();
    }

    @Test
    public void test() {
        refs = new IntRef[refCount];
        for (int k = 0; k < refCount; k++) {
            refs[k] = createIntRef(stm);
        }

        threads = new ChangeThread[threadCount];
        for (int k = 0; k < threadCount; k++) {
            threads[k] = new ChangeThread(k);
        }

        startAll(threads);
        sleepMs(getStressTestDurationMs(60 * 1000));
        stop = true;
        joinAll(threads);
    }

    public class ChangeThread extends TestThread {

        public ChangeThread(int id) {
            super("ChangeThread-" + id);
        }

        @Override
        public void doRun() throws Exception {
            int k = 0;
            while (!stop) {
                if (k % 100000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }
                transaction();
                k++;
            }
        }

        public void transaction() {
            new BetaTransactionTemplate(stm) {
                @Override
                public Object execute(BetaTransaction tx) throws Exception {
                    BetaObjectPool pool = getThreadLocalBetaObjectPool();
                    for (int k = 0; k < refs.length; k++) {
                        if (randomInt(3) == 0) {
                            int index = randomInt(refs.length);

                            if (randomInt(5) == 0) {
                                IntRef ref = refs[index];
                                ref.set( tx, pool,ref.get(tx, pool)+1);
                            }
                        }
                    }
                    return null;
                }
            }.execute();
        }
    }
}
