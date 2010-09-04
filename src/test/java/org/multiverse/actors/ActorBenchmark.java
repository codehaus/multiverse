package org.multiverse.actors;

import org.multiverse.stms.beta.BetaStmUtils;

import java.util.concurrent.TimeUnit;

/**
 * @author Peter Veentjer
 */
@SuppressWarnings({"FieldCanBeLocal"})
public class ActorBenchmark {

    private final long transactionCount = 200 * 1000 * 1000;

    public static void main(String[] args) throws InterruptedException {
        ActorBenchmark benchmark = new ActorBenchmark();
        benchmark.run();
    }

    private void run() throws InterruptedException {
        Actor actor = new TestActor();
        actor.start();

        long startNs = System.nanoTime();

        for (int k = 0; k < transactionCount; k++) {
            actor.send("foo");

            if (k % 10000000 == 0) {
                System.out.println("at message: " + k);
            }
        }

        actor.shutdown();

        long durationNs = System.nanoTime() - startNs;
        double transactionsPerSecond = (1.0d * transactionCount * TimeUnit.SECONDS.toNanos(1)) / durationNs;
        System.out.printf("Performance %s transactions/second\n", BetaStmUtils.format(transactionsPerSecond));
    }
}
