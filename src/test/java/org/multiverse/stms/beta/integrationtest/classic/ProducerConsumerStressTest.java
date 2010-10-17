package org.multiverse.stms.beta.integrationtest.classic;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicClosure;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.api.references.IntRef;

import javax.xml.transform.Templates;

import static junit.framework.Assert.assertEquals;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.StmUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * http://en.wikipedia.org/wiki/Producer-consumer_problem
 */
public class ProducerConsumerStressTest {

    private Buffer buffer;
    private volatile boolean stop;
    private static final int MAX_CAPACITY = 100;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        buffer = new Buffer();
        stop = false;
    }

    @Test
    public void test() {
        ProducerThread producerThread = new ProducerThread();
        ConsumerThread consumerThread = new ConsumerThread();

        startAll(producerThread,consumerThread);
        sleepMs(30 * 1000);
        stop = true;
        joinAll(producerThread,consumerThread);

        assertEquals(0,buffer.size.atomicGet());
    }

    public class ProducerThread extends TestThread {
        public ProducerThread() {
            super("ProducerThread");
        }

        @Override
        public void doRun() {
            int k = 0;
            while (!stop) {
                k++;
                buffer.put(k);

                if(k % 100000 == 0){
                    System.out.printf("%s is at %d\n", getName(),k);
                }
            }
            buffer.put(-1);
        }
    }

    public class ConsumerThread extends TestThread {
        public ConsumerThread() {
            super("ConsumerThread");
        }

        @Override
        public void doRun() {
            int item;
            int k = 0;
            do {
                item = buffer.take();
                k++;
                if(k % 100000 == 0){
                    System.out.printf("%s is at %d\n", getName(),k);
                }
            } while (item != -1);
        }
    }

    class Buffer {
        private final IntRef size = newIntRef();
        private final IntRef[] items;

        Buffer() {
            this.items = new IntRef[MAX_CAPACITY];
            for (int k = 0; k < items.length; k++) {
                items[k] = newIntRef();
            }
        }

        int take() {
            return execute(new AtomicClosure<Integer>() {
                @Override
                public Integer execute(Transaction tx) throws Exception {
                    if (size.get() == 0) {
                        retry();
                    }

                    size.decrement();
                    return items[size.get()].get();
                }
            });
        }

        void put(final int item) {
            execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    if (size.get() >= MAX_CAPACITY) {
                        retry();
                    }

                    items[size.get()].set(item);
                    size.increment();
                }
            });
        }
    }
}
