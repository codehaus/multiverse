package org.multiverse.stms.alpha.programmatic;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.TestUtils;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.ThreadLocalTransaction;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.programmatic.ProgrammaticLongRef;
import org.multiverse.api.programmatic.ProgrammaticRefFactory;
import org.multiverse.stms.alpha.AlphaStm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.TestUtils.startAll;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class AlphaProgrammaticLongRef_atomicBehaviorStressTest {

    private int modifyThreadCount = 10;
    private int transactionCount = 1000 * 1000;
    private int refCount = 100;
    private ProgrammaticLongRef[] refs;
    private AlphaStm stm;
    private ProgrammaticRefFactory refFactory;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stm = (AlphaStm) getGlobalStmInstance();
        refFactory = stm.getProgrammaticRefFactoryBuilder().build();

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
    public void test_withExplicitAborts() {
        test(true);
    }

    @Test
    public void test_withNoExplicitAborts() {
        test(false);
    }

    public void test(boolean withAborts) {
        ModifyThread[] modifyThreads = new ModifyThread[modifyThreadCount];
        for (int k = 0; k < modifyThreadCount; k++) {
            modifyThreads[k] = new ModifyThread(k, withAborts);
        }

        startAll(modifyThreads);
        joinAll(modifyThreads);
        assertEquals(sumThreads(modifyThreads), sumRefs());
    }

    public long sumRefs() {
        long result = 0;
        for (ProgrammaticLongRef ref : refs) {
            result += ref.atomicGet();
        }
        return result;
    }

    public long sumThreads(ModifyThread[] threads) {
        long result = 0;
        for (ModifyThread thread : threads) {
            result += thread.incCount;
        }
        return result;
    }

    public class ModifyThread extends TestThread {
        private final boolean withAborts;
        private long incCount;

        public ModifyThread(int id, boolean withAborts) {
            super("ModifyThread-" + id);
            this.withAborts = withAborts;
        }

        @Override
        public void doRun() throws Exception {
            for (int k = 0; k < transactionCount; k++) {
                if (k % 100000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }

                boolean abort = withAborts && k % 2 == 0;
                if (abort) {
                    try {
                        incRefsButAbort();
                        fail();
                    } catch (DeadTransactionException ignore) {
                    }
                } else {
                    incCount += incRefs();
                }
            }
        }

        @TransactionalMethod
        private long incRefs() {
            return modifyInternal();
        }

        @TransactionalMethod
        private long incRefsButAbort() {
            modifyInternal();
            ThreadLocalTransaction.getThreadLocalTransaction().abort();
            return 0;//won't be called
        }

        private long modifyInternal() {
            long incCount = 0;
            for (ProgrammaticLongRef ref : refs) {
                if (TestUtils.randomOneOf(10)) {
                    ref.inc(1);
                    incCount++;
                }
            }
            return incCount;
        }
    }
}
