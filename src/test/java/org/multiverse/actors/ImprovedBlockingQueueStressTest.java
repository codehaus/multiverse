package org.multiverse.actors;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStmUtils;

import java.util.concurrent.TimeUnit;

import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class ImprovedBlockingQueueStressTest {

    private final long transactionCount = 50 * 1000 * 1000;

    @Before
    public void setUp(){
        clearThreadLocalTransaction();
    }

    @Test
    public void run() throws InterruptedException {
        ImprovedBlockingQueue queue = new ImprovedBlockingQueue(1000);
        long startNs = System.nanoTime();

        ImprovedBlockingQueue.PutClosure putClosure = queue.createPutClosure();

        for (int k = 0; k < transactionCount; k++) {
            putClosure.item = "foo";
            queue.put(putClosure);
            queue.take();

            if (k % 10000000 == 0) {
                System.out.println("at message: " + k);
            }
        }

        long durationNs = System.nanoTime() - startNs;
        double transactionsPerSecond = (1.0d * transactionCount * TimeUnit.SECONDS.toNanos(1)) / durationNs;
        System.out.printf("Performance %s transactions/second\n", BetaStmUtils.format(transactionsPerSecond));
    }
}
