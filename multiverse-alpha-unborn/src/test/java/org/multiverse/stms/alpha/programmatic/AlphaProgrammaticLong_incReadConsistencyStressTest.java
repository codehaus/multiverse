package org.multiverse.stms.alpha.programmatic;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.Stm;
import org.multiverse.api.programmatic.ProgrammaticLong;
import org.multiverse.api.programmatic.ProgrammaticReferenceFactory;

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
public class AlphaProgrammaticLong_incReadConsistencyStressTest {

    private int refCount = 10;
    private volatile boolean completed;
    private ProgrammaticLong[] refs;
    private Stm stm;
    private int transactionCount = 1000 * 1000;
    private int readerCount = 10;

    @Before
    public void setUp() {
        completed = false;
        clearThreadLocalTransaction();
        stm = getGlobalStmInstance();
        ProgrammaticReferenceFactory refFactory = stm.getProgrammaticReferenceFactoryBuilder()
                .build();

        refs = new ProgrammaticLong[refCount];
        for (int k = 0; k < refs.length; k++) {
            refs[k] = refFactory.atomicCreateLong(0);
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

        joinAll(readThreads);
        joinAll(incThread);
    }

    public class ReadThread extends TestThread {
        public ReadThread(int id) {
            super("ReadThread-" + id);
        }

        @Override
        public void doRun() throws Exception {
            for (int k = 0; k < transactionCount; k++) {
                read();
                sleepRandomUs(10);

                if (k % 100000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }
            }
            completed = true;
        }

        @TransactionalMethod(readonly = true, trackReads = false)
        private void read() {
            int sumT1 = 0;
            for (ProgrammaticLong ref : refs) {
                sumT1 += ref.get();
            }

            sleepRandomUs(10);

            int sumT2 = 0;
            for (ProgrammaticLong ref : refs) {
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
            while (!completed) {
                sleepRandomUs(10);
                if (k % 100000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }
                k++;
            }
        }

        public void inc() {
            for (int k = 0; k < refs.length; k++) {
                if (randomOneOf(5)) {
                    if (commute) {
                        refs[k].commutingInc(1);
                    } else {
                        refs[k].atomicInc(1);
                    }
                }
            }
        }
    }
}
