package org.multiverse.stms.alpha.programmatic;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.programmatic.ProgrammaticReferenceFactory;
import org.multiverse.stms.alpha.AlphaStm;

import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.TestUtils.startAll;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.StmUtils.retry;

/**
 * @author Peter Veentjer
 */
public class AlphaProgrammaticReference_blockingStressTest {

    private AlphaStm stm;
    private ProgrammaticReferenceFactory refFactory;
    private AlphaProgrammaticReference ref;
    private int consumerCount = 10;
    private int transactionCount = 1000 * 1000 * 2;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
        refFactory = stm.getProgrammaticReferenceFactoryBuilder()
                .build();
        ref = (AlphaProgrammaticReference) refFactory.atomicCreateReference(0);
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
        joinAll(producers);
        joinAll(consumers);
    }


    class ProducerThread extends TestThread {
        public ProducerThread(int id) {
            super("ProducerThread-" + id);
        }

        @Override
        public void doRun() throws Exception {
            for (int k = 0; k < transactionCount; k++) {
                produce();

                if (k % 100000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }
            }
        }

        @TransactionalMethod(readonly = false)
        private void produce() {
            if (ref.get() != null) {
                retry();
            }

            ref.set("foo");
        }
    }

    class ConsumerThread extends TestThread {
        public ConsumerThread(int id) {
            super("ConsumerThread-" + id);
        }

        @Override
        public void doRun() throws Exception {
            for (int k = 0; k < transactionCount; k++) {
                consume();

                if (k % 100000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }
            }
        }

        @TransactionalMethod(readonly = false)
        private void consume() {
            if (ref.get() == null) {
                retry();
            }

            ref.set(null);
        }
    }
}
