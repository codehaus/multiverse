package org.multiverse.stms.alpha.programmatic;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.TestUtils;
import org.multiverse.api.programmatic.ProgrammaticRefFactory;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.utils.ThreadLocalRandom;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import static java.lang.Math.abs;
import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class AlphaProgrammaticLongRef_atomicStressTest {
    private volatile boolean stop;
    private AlphaStm stm;
    private AlphaProgrammaticLongRef[] refs;
    private int refCount = 1000;
    private int threadCount = 2;
    private ProgrammaticRefFactory refFactory;


    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
        refFactory = stm.getProgrammaticRefFactoryBuilder()
                .build();
        clearThreadLocalTransaction();
        stop = false;
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @Test
    public void test() {
        refs = createRefs();

        AtomicIncThread[] threads = createThreads();

        long startNs = System.nanoTime();
        startAll(threads);
        sleepMs(TestUtils.getStressTestDurationMs(60*1000));
        stop = true;
        joinAll(threads);

        long totalIncCount = sum(threads);
        assertEquals(totalIncCount, sumRefs());

        long durationNs = System.nanoTime() - startNs;
        double transactionsPerSecond = (1.0d * totalIncCount * TimeUnit.SECONDS.toNanos(1)) / durationNs;
        System.out.printf("Performance %s transactions/second\n", format(transactionsPerSecond));
    }

    private long sumRefs() {
        long result = 0;
        for (AlphaProgrammaticLongRef ref : refs) {
            result += ref.get();
        }
        return result;
    }

    private long sum(AtomicIncThread[] threads){
        long result = 0;
        for(AtomicIncThread t: threads){
            result+=t.count;
        }
        return result;
    }

    private AlphaProgrammaticLongRef[] createRefs() {
        AlphaProgrammaticLongRef[] refs = new AlphaProgrammaticLongRef[refCount];
        for (int k = 0; k < refCount; k++) {
            refs[k] = (AlphaProgrammaticLongRef) refFactory.atomicCreateLongRef(0);
        }
        return refs;
    }

    private AtomicIncThread[] createThreads() {
        AtomicIncThread[] threads = new AtomicIncThread[threadCount];
        for (int k = 0; k < threadCount; k++) {
            threads[k] = new AtomicIncThread(k + 1);
        }
        return threads;
    }

    public class AtomicIncThread extends TestThread {
        private long count;

        public AtomicIncThread(int id) {
            super("AtomicIncThread-" + id);
        }

        @Override
        public void doRun() throws Exception {
            Random random = ThreadLocalRandom.current();

            while(!stop){
                int refIndex = abs(abs(random.nextInt()) % refs.length);

                if (refIndex < 0) {
                    System.out.println("refIndex: " + refIndex);
                }

                refs[refIndex].atomicInc(1);

                if (count % (10 * 1000 * 1000) == 0) {
                    System.out.printf("%s is at %s\n", getName(), count);
                }
                count++;
            }
        }
    }
}
