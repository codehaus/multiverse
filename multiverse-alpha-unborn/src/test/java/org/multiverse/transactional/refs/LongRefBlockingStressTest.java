package org.multiverse.transactional.refs;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.TestUtils;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.stms.alpha.AlphaStm;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.StmUtils.retry;

/**
 * @author Peter Veentjer
 */
public class LongRefBlockingStressTest {

     private AlphaStm stm;
    private LongRef ref;
    private BooleanRef completedRef;
    private int consumerCount = 10;
    private int unprocessedCapacity = 1000;
    private volatile boolean stop;

    @Before
    public void setUp() {
        stop = false;
        stm = (AlphaStm) getGlobalStmInstance();
        ref = new LongRef();
        completedRef = new BooleanRef();
    }

    @Test
    public void test() {
        ProducerThread[] producers = new ProducerThread[consumerCount];
        for (int k = 0; k < producers.length; k++) {
            producers[k] = new ProducerThread(k);
        }

        ConsumerThread[] consumers = new ConsumerThread[consumerCount];
        for (int k = 0; k < producers.length; k++) {
            consumers[k] = new ConsumerThread(k);
        }

        startAll(producers);
        startAll(consumers);

        sleepMs(TestUtils.getStressTestDurationMs(60 * 1000));
        stop = true;

        joinAll(producers);
        joinAll(consumers);

        long produceCount = sum(producers);
        long consumeCount = sum(consumers);
        System.out.println("missing consumes: "+(produceCount-consumeCount));
        assertEquals(produceCount, consumeCount);
    }

    long sum(ProducerThread[] threads) {
        long result = 0;
        for (ProducerThread thread : threads) {
            result += thread.count;
        }
        return result;
    }

    long sum(ConsumerThread[] threads) {
        long result = 0;
        for (ConsumerThread thread : threads) {
            result += thread.count;
        }
        return result;
    }

    class ProducerThread extends TestThread {
        long count = 0;

        public ProducerThread(int id) {
            super("ProducerThread-" + id);
        }

        @Override
        public void doRun() throws Exception {
            while (!stop) {
                if (produce()) {
                    count++;
                }

                if (count % 100000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), count);
                }
            }

            completedRef.set(true);
        }

        @TransactionalMethod(readonly = false)
        private boolean produce() {
            if(completedRef.get()){
                return false;
            }

            long value = ref.get();

            if (value >= unprocessedCapacity) {
                retry();
            }

            ref.inc();

            return true;
        }
    }

    class ConsumerThread extends TestThread {
        long count = 0;

        public ConsumerThread(int id) {
            super("ConsumerThread-" + id);
        }

        @Override
        public void doRun() throws Exception {
            boolean again;
            do {
                again = consume();

                if (count % 100000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), count);
                }

                if (again) {
                    count++;
                }
            } while (again);
        }

        @TransactionalMethod(readonly = false)
        private boolean consume() {
            if(completedRef.get()){
                return false;
            }

            long value = ref.get();

            if (value == 0) {
                retry();
            }

            ref.inc(-1);
            return true;
        }

    }

}
