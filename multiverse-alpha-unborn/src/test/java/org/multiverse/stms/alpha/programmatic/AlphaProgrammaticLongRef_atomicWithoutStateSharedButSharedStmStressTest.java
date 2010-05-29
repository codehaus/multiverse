package org.multiverse.stms.alpha.programmatic;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.programmatic.ProgrammaticRefFactory;
import org.multiverse.stms.alpha.AlphaStm;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class AlphaProgrammaticLongRef_atomicWithoutStateSharedButSharedStmStressTest {

    private AlphaStm stm;
    private AlphaProgrammaticLongRef[] refs;
    private int threadCount = 8;
    private long incCountPerThread = 1000 * 1000 * 20;
    private ProgrammaticRefFactory refFactory;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
        refFactory = stm.getProgrammaticRefFactoryBuilder()
                .build();
        clearThreadLocalTransaction();
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
        joinAll(threads);

        long totalIncCount = threadCount * incCountPerThread;
        assertEquals(totalIncCount, sum());

        long durationNs = System.nanoTime() - startNs;
        double transactionsPerSecond = (1.0d * totalIncCount * TimeUnit.SECONDS.toNanos(1)) / durationNs;
        System.out.printf("Performance %s transactions/second\n", format(transactionsPerSecond));

    }

    private long sum() {
        long result = 0;
        for (AlphaProgrammaticLongRef ref : refs) {
            result += ref.get();
        }
        return result;
    }

    private AlphaProgrammaticLongRef[] createRefs() {
        AlphaProgrammaticLongRef[] refs = new AlphaProgrammaticLongRef[threadCount];
        for (int k = 0; k < threadCount; k++) {
            refs[k] = (AlphaProgrammaticLongRef) refFactory.atomicCreateLongRef(0);
        }
        return refs;
    }

    private AtomicIncThread[] createThreads() {
        AtomicIncThread[] threads = new AtomicIncThread[threadCount];
        for (int k = 0; k < threadCount; k++) {
            threads[k] = new AtomicIncThread(k);
        }
        return threads;
    }

    public class AtomicIncThread extends TestThread {
        private int id;

        public AtomicIncThread(int id) {
            super("AtomicIncThread-" + id);
            this.id = id;
        }

        @Override
        public void doRun() throws Exception {
            for (int k = 0; k < incCountPerThread; k++) {
                refs[id].atomicInc(1);

                if (k % (1000 * 1000) == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }
            }
        }
    }

}
