package org.multiverse.stms.beta.integrationtest;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.benchmarks.BenchmarkUtils;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.StmUtils;
import org.multiverse.stms.beta.ObjectPool;
import org.multiverse.stms.beta.TransactionTemplate;
import org.multiverse.stms.beta.refs.LongRef;
import org.multiverse.stms.beta.refs.LongRefTranlocal;
import org.multiverse.stms.beta.transactions.BetaTransaction;
import org.multiverse.stms.beta.transactions.BetaTransactionConfig;
import org.multiverse.stms.beta.transactions.MonoBetaTransaction;

import static java.lang.System.currentTimeMillis;
import static org.junit.Assert.assertEquals;

/**
 * @author Peter Veentjer
 */
public class IsolationTest {

    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void optimistic() throws InterruptedException {
        test(false);
    }

    @Test
    public void pessimistic() throws InterruptedException {
        test(true);
    }

    public void test(boolean pessimistic) throws InterruptedException {
        int threadCount = 2;
        UpdateThread[] threads = new UpdateThread[threadCount];
        LongRef ref = StmUtils.createLongRef(stm);
        long txCount = 100 * 1000 * 1000;

        for (int k = 0; k < threads.length; k++) {
            threads[k]=new UpdateThread(k,ref, txCount,pessimistic);
        }

        for(UpdateThread thread: threads){
            thread.start();
        }

        long durationMs = 0;
        for(UpdateThread thread: threads){
            thread.join();
            durationMs+=thread.durationMs;
        }

        double performance = BenchmarkUtils.perSecond(txCount, durationMs, threadCount);
        System.out.printf("Performance %s transactions/second\n", BenchmarkUtils.format(performance));
        assertEquals(threadCount * txCount, ref.active.value);
        System.out.println("ref.orec: "+ref.toOrecString());
    }

    class UpdateThread extends Thread {
        private final LongRef ref;
        private final long count;
        private final boolean pessimistic;
        private long durationMs;

        public UpdateThread(int id, LongRef ref, long count, boolean pessimistic) {
            super("UpdateThread-" + id);
            this.ref = ref;
            this.count = count;
            this.pessimistic = pessimistic;
        }

        @Override
        public void run() {
            final ObjectPool pool = new ObjectPool();

            final BetaTransactionConfig config = new BetaTransactionConfig(stm);
            TransactionTemplate template = new TransactionTemplate(stm, new MonoBetaTransaction(config)) {
                @Override
                public void execute(BetaTransaction tx) {
                    //todo: cast can be removed as soon as all transactions are 'specialized'
                    LongRefTranlocal tranlocal = (LongRefTranlocal) tx.openForWrite(ref, pessimistic,pool);
                    tranlocal.value++;
                }
            };

            long startMs = currentTimeMillis();

            for (long k = 0; k < count; k++) {
                template.execute(pool);
            }

            durationMs = currentTimeMillis()-startMs;

            System.out.printf("finished %s after %s ms\n",getName(),durationMs);
        }
    }
}
