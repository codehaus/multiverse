package org.multiverse.transactional.refs;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.TestUtils;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.Transaction;
import org.multiverse.templates.TransactionTemplate;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class BasicRefBlockingStressTest {

    private BasicRef<String> ref;
    private int consumerCount = 10;
    private volatile boolean stop;
    private String poison = "poison";

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stop = false;
        ref = new BasicRef<String>();
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

        long producedCount = sum(producers);
        long consumedCount = sum(consumers);
        assertEquals(producedCount, consumedCount);
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

                if (count % 10000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), count);
                }
            }

            new TransactionTemplate() {
                @Override
                public Object execute(Transaction tx) throws Exception {
                    if (poison.equals(ref.get())) {
                        return null;
                    }

                    if (ref.get() != null) {
                        retry();
                    }

                    ref.set(poison);
                    return null;
                }
            }.execute();
        }

        @TransactionalMethod(readonly = false)
        private boolean produce() {
            String value = ref.get();

            if (poison.equals(value)) {
                return false;
            }

            if (value != null) {
                retry();
            }

            ref.set("token");
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

                if (count % 10000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), count);
                }

                if (again) {
                    count++;
                }
            } while (again);
        }

        @TransactionalMethod(readonly = false)
        private boolean consume() {
            String value = ref.get();

            if (poison.equals(value)) {
                return false;
            }

            if (value == null) {
                retry();
            }

            ref.set(null);
            return true;
        }
    }
}
