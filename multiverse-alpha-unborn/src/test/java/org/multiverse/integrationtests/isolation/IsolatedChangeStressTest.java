package org.multiverse.integrationtests.isolation;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.transactional.refs.LongRef;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class IsolatedChangeStressTest {

    private int threadCount = 4;
    private volatile boolean stop;

    private LongRef ref;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stop = false;
        ref = new LongRef();
    }

    @Test
    public void test() {
        WriteThread[] threads = new WriteThread[threadCount];
        for(int k=0;k<threads.length;k++){
            threads[k]=new WriteThread(k);
        }

        startAll(threads);
        sleepMs(getStressTestDurationMs(60 * 1000));
        stop = true;
        joinAll(threads);

        assertEquals(sum(threads), ref.get());
        System.out.println("end value is : "+ref);
    }

    public long sum(WriteThread[] threads) {
        long sum = 0;
        for (WriteThread thread : threads) {
            sum += thread.count;
        }
        return sum;
    }

    class WriteThread extends TestThread {

        private long count = 0;

        public WriteThread(int id) {
            super("WriteThread-" + id);
        }

        @Override
        public void doRun() throws Exception {
            while(!stop){
                ref.inc();
                count++;

                if(count % 5000000 == 0){
                    System.out.printf("%s is at %s\n", getName(), count);
                }
            }
        }
    }
}
