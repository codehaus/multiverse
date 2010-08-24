package org.multiverse.stms.beta.orec;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.stms.beta.conflictcounters.GlobalConflictCounter;
import org.multiverse.stms.beta.transactionalobjects.LongRef;

import static org.multiverse.TestUtils.*;

public class FastOrecStressTest {
    private FastOrec orec;
    private int threadCount = 2;
    private int spinCount = 16;
    private GlobalConflictCounter globalConflictCounter;
    private LongRef dummyRef;
    private volatile boolean stop;
    private boolean readBiased;

    @Before
    public void setUp() {
        orec = new FastOrec();
        globalConflictCounter = new GlobalConflictCounter(1);
        dummyRef = new LongRef();
        stop = false;
        readBiased = false;
    }

    @Test
    public void test() {
        StressThread[] threads = new StressThread[threadCount];
        for (int k = 0; k < threads.length; k++) {
            threads[k] = new StressThread(k);
        }

        startAll(threads);
        sleepMs(30 * 1000);

        stop = true;
        joinAll(threads);

        System.out.println("orec: "+orec.___toOrecString());
    }

    public class StressThread extends TestThread {
        public StressThread(int id) {
            super("StressThread-" + id);
        }

        @Override
        public void doRun() throws Exception {
            while (!stop) {
                switch (randomInt(2)) {
                    case 0:
                        doRead();
                        break;
                    case 1:
                        doUpdate();
                        break;
                    default:
                        throw new IllegalStateException();
                }
            }
        }

        public void doRead() {
            if (!orec.___arrive(spinCount)) {
                return;
            }

            sleepRandomUs(10);

            if (!readBiased) {
                if (orec.___departAfterReading()) {
                    readBiased = true;
                    orec.___releaseLockAfterBecomingReadBiased();
                }
            }
        }

        //public void doFailInTheBeginningUpdate() {
        //    if (!orec.___arrive(spinCount)) {
        //        return;
        //    }
        //
        //    sleepRandomUs(10);
        //    orec.___departAfterFailure();
        //}

        public void doUpdate() {
           if (!orec.___arrive(spinCount)) {
               return;
           }

           sleepRandomUs(10);

           if (!orec.___tryUpdateLock(spinCount)) {
               orec.___departAfterFailure();
               return;
           }
           sleepRandomUs(10);
            
           readBiased = false;
           orec.___departAfterUpdateAndReleaseLock(globalConflictCounter, dummyRef);
        }
    }
}
