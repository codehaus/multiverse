package org.multiverse.commitbarriers;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.Transaction;
import org.multiverse.transactional.refs.IntRef;

import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

public class CountDownCommitBarrier_StressTest {

    private AtomicLong totalInc;
    private AtomicLong commitInc;

    private int oneOfFails = 4;

    private int refCount = 50;
    private int maxPartiesCount = 5;
    private int spawnCountPerThread = 2 * 1000;
    private int spawnCount = 5;

    private IntRef[] refs;
    private ThreadPoolExecutor executor =
            new ThreadPoolExecutor(50, 50, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    private ThreadPoolExecutor spawnExecutor =
            new ThreadPoolExecutor(spawnCount, spawnCount, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

    @Before
    public void setUp() {
        commitInc = new AtomicLong();
        totalInc = new AtomicLong();
        refs = new IntRef[refCount];
        for (int k = 0; k < refCount; k++) {
            refs[k] = new IntRef();
        }
    }

    @Test
    public void test() throws InterruptedException, TimeoutException {
        for (int k = 0; k < spawnCount; k++) {
            spawnExecutor.execute(new SpawnTask("SpawnTask-" + k));
        }

        Runnable shutdownTask = new Runnable() {
            @Override
            public void run() {
                spawnExecutor.shutdown();
            }
        };
        spawnExecutor.execute(shutdownTask);
        if (!spawnExecutor.awaitTermination(5, TimeUnit.MINUTES)) {
            fail("failed to complete test, it took too long");
        }

        System.out.printf("commitInc %s totalInc %s\n", commitInc.get(), totalInc.get());
        assertEquals(commitInc.get(), sum());
    }

    public long sum() {
        long sum = 0;
        for (int k = 0; k < refCount; k++) {
            sum += refs[k].get();
        }
        return sum;
    }

    public class SpawnTask implements Runnable {

        private String name;

        public SpawnTask(String name) {
            this.name = name;
        }

        @Override
        public void run() {
            for (int k = 0; k < spawnCountPerThread; k++) {
                runOnce();

                if (k % 100 == 0) {
                    System.out.println(name + " is at " + k);
                }
            }
        }

        public void runOnce() {
            int partyCount = randomInt(maxPartiesCount) + 1;
            totalInc.addAndGet(partyCount);
            CountDownCommitBarrier countDownCommitBarrier = new CountDownCommitBarrier(partyCount);

            Vector<Transaction> transactions = new Vector<Transaction>();
            for (int k = 0; k < partyCount; k++) {
                executor.execute(new WorkerTask(k == 0, countDownCommitBarrier, transactions));
            }

            countDownCommitBarrier.awaitOpenUninterruptibly();

            if (countDownCommitBarrier.isCommitted()) {
                commitInc.getAndAdd(partyCount);
            }
        }
    }

    class WorkerTask implements Runnable {
        final CountDownCommitBarrier countDownCommitBarrier;
        final boolean first;
        private Vector<Transaction> transactions;

        WorkerTask(boolean first, CountDownCommitBarrier countDownCommitBarrier, Vector<Transaction> transactions) {
            this.countDownCommitBarrier = countDownCommitBarrier;
            this.transactions = transactions;
            this.first = first;
        }

        @Override
        public void run() {
            try {
                clearThreadLocalTransaction();
                doRun();
            } catch (IllegalStateException ignore) {
            }
        }

        @TransactionalMethod
        public void doRun() {
            sleepRandomMs(10);
            refs[randomInt(refs.length)].inc();
            sleepRandomMs(10);

            Transaction tx = getThreadLocalTransaction();
            transactions.add(tx);

            if (first && randomOneOf(oneOfFails)) {
                countDownCommitBarrier.abort();
            }

            countDownCommitBarrier.joinCommitUninterruptibly(tx);
        }
    }
}
