package org.multiverse.stms.alpha.programmatic;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.Stm;
import org.multiverse.api.programmatic.ProgrammaticLongRef;
import org.multiverse.api.programmatic.ProgrammaticRefFactory;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * Tests if the commuting inc can cause read inconsistencies. This test is added because there was
 * a readconsistency problem with the TransactionalLinkedList.
 *
 * @author Peter Veentjer
 */
public class AlphaProgrammaticLongRef_incReadConsistencyStressTest {

    private volatile boolean stop;
    private int refCount = 10;
    private ProgrammaticLongRef[] refs;
    private Stm stm;
    private int readerCount = 10;

    @Before
    public void setUp() {
        stop = false;
        clearThreadLocalTransaction();
        stm = getGlobalStmInstance();
        ProgrammaticRefFactory refFactory = stm.getProgrammaticRefFactoryBuilder()
                .build();

        refs = new ProgrammaticLongRef[refCount];
        for (int k = 0; k < refs.length; k++) {
            refs[k] = refFactory.atomicCreateLongRef(0);
        }
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @Test
    public void testWithCommute() {
        test(true);
    }

    @Test
    public void testWithoutCommit() {
        test(false);
    }

    public void test(boolean commute) {
        CommutingIncThread incThread = new CommutingIncThread(commute);

        ReadThread[] readThreads = new ReadThread[readerCount];
        for (int k = 0; k < readThreads.length; k++) {
            readThreads[k] = new ReadThread(1);
        }

        startAll(incThread);
        startAll(readThreads);

        sleepMs(getStressTestDurationMs(60*1000));
        stop = true;
        joinAll(readThreads);
        joinAll(incThread);
    }

    public class ReadThread extends TestThread {
        private long count;

        public ReadThread(int id) {
            super("ReadThread-" + id);
        }

        @Override
        public void doRun() throws Exception {
            while(!stop){
                read();
                sleepRandomUs(10);

                if (count % 100000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), count);
                }
                count++;
            }
        }

        @TransactionalMethod(readonly = true, trackReads = false)
        private void read() {
            int sumT1 = 0;
            for (ProgrammaticLongRef ref : refs) {
                sumT1 += ref.get();
            }

            sleepRandomUs(10);

            int sumT2 = 0;
            for (ProgrammaticLongRef ref : refs) {
                sumT2 += ref.get();
            }

            assertEquals(sumT1, sumT2);
        }
    }

    public class CommutingIncThread extends TestThread {
        private final boolean commute;

        public CommutingIncThread(boolean commute) {
            super("IncThread");
            this.commute = commute;
        }

        @Override
        public void doRun() throws Exception {
            int k = 0;
            while (!stop) {
                sleepRandomUs(10);
                if (k % 100000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }
                k++;
            }
        }

        public void inc() {
            for (ProgrammaticLongRef ref : refs) {
                if (randomOneOf(5)) {
                    if (commute) {
                        ref.commutingInc(1);
                    } else {
                        ref.atomicInc(1);
                    }
                }
            }
        }
    }
}
