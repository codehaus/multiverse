package org.multiverse.utils.clock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.multiverse.TestThread;
import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.TestUtils.startAll;

import static java.lang.Math.max;

/**
 * Executes a stress test on the {@link org.multiverse.utils.clock.StrictClock} to see
 * that it behaves correctly.
 * 
 * @author Peter Veentjer
 */
public class StrictClockLongTest {

    private final long tickCount = 10 * 1000 * 1000;
    private final Clock clock = new StrictClock();
    //since the test would not make sense using a single thread, the minimal number of
    //thread is 2 (but who has a single core system these days).
    private final int threadCount = max(Runtime.getRuntime().availableProcessors(), 2);

    @Test
    public void test() {
        System.out.printf("StrictClockLongTest threadCount=%s  tickCount/thread=%s\n",threadCount, tickCount);
        
        TickThread[] threads = createThreads();
        startAll(threads);
        joinAll(threads);

        long expectedTime = tickCount * threadCount;
        assertEquals(expectedTime, clock.getVersion());
    }

    public TickThread[] createThreads() {
        TickThread[] threads = new TickThread[threadCount];
        for (int k = 0; k < threads.length; k++) {
            threads[k] = new TickThread(k);
        }
        return threads;
    }

    class TickThread extends TestThread {

        public TickThread(int id) {
            super("TickThread-" + id);
        }

        @Override
        public void doRun() {
            long previousTime = clock.getVersion();

            for (long k = 0; k < tickCount; k++) {
                long nextTime = clock.tick();
                if (nextTime <= previousTime) {
                    fail();
                }
                previousTime = nextTime;
            }
        }
    }
}
