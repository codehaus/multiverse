package org.multiverse.stms.alpha.programmatic;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.TestUtils;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.programmatic.ProgrammaticRefFactory;
import org.multiverse.stms.alpha.AlphaStm;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.StmUtils.retry;

/**
 * @author Peter Veentjer
 */
public class AlphaProgrammaticLongRef_blockingStressTest {
    private AlphaStm stm;
    private ProgrammaticRefFactory refFactory;
    private AlphaProgrammaticLongRef ref;
    private int consumerCount = 10;
    private int unprocessedCapacity = 1000;
    private volatile boolean stop;

    @Before
    public void setUp() {
        stop = false;
        stm = (AlphaStm) getGlobalStmInstance();
        refFactory = stm.getProgrammaticRefFactoryBuilder()
                .build();
        ref = (AlphaProgrammaticLongRef) refFactory.atomicCreateLongRef(0);
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

        assertEquals(sum(producers), sum(consumers));
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

                if (count % 1000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), count);
                }
            }

            ref.set(-1);
        }

        @TransactionalMethod(readonly = false)
        private boolean produce() {
            long value = ref.get();

            if (value < -1) {
                throw new RuntimeException();
            }

            if (value == -1) {
                return false;
            }

            if (value > unprocessedCapacity) {
                throw new RuntimeException();
            }

            if (value >= unprocessedCapacity) {
                retry();
            }

            ref.inc(1);
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
            boolean again = true;
            do {
                again = consume();

                if (count % 1000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), count);
                }

                if (again) {
                    count++;
                }
            } while (again);
        }

        @TransactionalMethod(readonly = false)
        private boolean consume() {
            long value = ref.get();

            if (value < -1) {
                throw new RuntimeException();
            }

            if (value == -1) {
                return false;
            }

            if (value == 0) {
                retry();
            }

            ref.inc(-1);
            return true;
        }
    }
}
