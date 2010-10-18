package org.multiverse.stms.beta.integrationtest.classic;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.TestUtils;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.IsolationLevel;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicBooleanClosure;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.api.references.BooleanRef;
import org.multiverse.api.references.Ref;

import static org.multiverse.TestUtils.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.StmUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * http://en.wikipedia.org/wiki/Cigarette_smokers_problem
 */
public class CigaretteSmokersProblemTest {

    private BooleanRef tobaccoAvailable;
    private BooleanRef paperAvailable;
    private BooleanRef matchesAvailable;
    private Ref<Thread> notifiedThread;
    private ArbiterThread arbiterThread;
    private PaperProviderThread paperProvider;
    private MatchProviderThread matchProvider;
    private TobaccoProviderThread tobaccoProvider;
    private volatile boolean stop;
    private AtomicBlock block;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        tobaccoAvailable = newBooleanRef(false);
        paperAvailable = newBooleanRef(false);
        matchesAvailable = newBooleanRef(false);
        notifiedThread = newRef();
        arbiterThread = new ArbiterThread();
        paperProvider = new PaperProviderThread();
        matchProvider = new MatchProviderThread();
        tobaccoProvider = new TobaccoProviderThread();
        stop = false;

        block = getGlobalStmInstance()
            .createTransactionFactoryBuilder()
            //.setPessimisticLockLevel(PessimisticLockLevel.PrivatizeReads)
            .setIsolationLevel(IsolationLevel.Serializable)
            .buildAtomicBlock();
    }

    @Test
    public void test() {
        startAll(arbiterThread, paperProvider, matchProvider, tobaccoProvider);
        sleepMs(560000);
        System.out.println("Stopping threads");
        stop = true;
        joinAll(arbiterThread, paperProvider, matchProvider, tobaccoProvider);
    }

    class ArbiterThread extends TestThread {
        public ArbiterThread() {
            super("Arbiter");
        }

        @Override
        public void doRun() throws Exception {
            while (!stop) {
                switch (TestUtils.randomInt(3)) {
                    case 0:
                        block.execute(new AtomicVoidClosure() {
                            @Override
                            public void execute(Transaction tx) {
                                if (notifiedThread.get() != null) {
                                    retry();
                                }

                                tobaccoAvailable.set(true);
                                paperAvailable.set(true);
                                notifiedThread.set(matchProvider);
                            }
                        });
                        break;
                    case 1:
                        block.execute(new AtomicVoidClosure() {
                            @Override
                            public void execute(Transaction tx) {
                                if (notifiedThread.get() != null) {
                                    retry();
                                }

                                tobaccoAvailable.set(true);
                                matchesAvailable.set(true);
                                notifiedThread.set(paperProvider);
                            }
                        });
                        break;
                    case 2:
                        block.execute(new AtomicVoidClosure() {
                            @Override
                            public void execute(Transaction tx) {
                                if (notifiedThread.get() != null) {
                                    retry();
                                }

                                matchesAvailable.set(true);
                                paperAvailable.set(true);
                                notifiedThread.set(tobaccoProvider);
                            }
                        });
                        break;
                    default:
                        throw new RuntimeException();
                }
            }
            notifiedThread.atomicSet(arbiterThread);
        }
    }

    class PaperProviderThread extends TestThread {
        public PaperProviderThread() {
            super("PaperProvidingSmoker");
        }

        @Override
        public void doRun() throws Exception {
            int k = 0;
            while (makeCigarette()) {
                sleepRandomMs(10);
                k++;
                if (k % 100 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }
            }
        }

        private boolean makeCigarette() {
            return block.execute(new AtomicBooleanClosure() {
                @Override
                public boolean execute(Transaction tx) throws Exception {
                    if (notifiedThread.get() == arbiterThread) {
                        return false;
                    }

                    if (notifiedThread.get() != PaperProviderThread.this) {
                        retry();
                    }

                    matchesAvailable.set(false);
                    tobaccoAvailable.set(false);
                    notifiedThread.set(null);
                    return true;
                }
            });
        }
    }

    class MatchProviderThread extends TestThread {
        public MatchProviderThread() {
            super("MatchProvidingSmoker");
        }

        @Override
        public void doRun() throws Exception {
            int k = 0;
            while (makeCigarette()) {
                sleepRandomMs(10);
                k++;
                if (k % 100 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }
            }
        }

        private boolean makeCigarette() {
            return block.execute(new AtomicBooleanClosure() {
                @Override
                public boolean execute(Transaction tx) throws Exception {
                    if (notifiedThread.get() == arbiterThread) {
                        return false;
                    }

                    if (notifiedThread.get() != MatchProviderThread.this) {
                        retry();
                    }

                    paperAvailable.set(false);
                    tobaccoAvailable.set(false);
                    notifiedThread.set(null);
                    return true;
                }
            });
        }
    }

    class TobaccoProviderThread extends TestThread {
        public TobaccoProviderThread() {
            super("TobaccoProvidingSmoker");
        }

        @Override
        public void doRun() throws Exception {
            int k = 0;
            while (makeCigarette()) {
                sleepRandomMs(10);

                k++;
                if (k % 100 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }
            }
        }

        private boolean makeCigarette() {
            return block.execute(new AtomicBooleanClosure() {
                @Override
                public boolean execute(Transaction tx) throws Exception {
                    if (notifiedThread.get() == arbiterThread) {
                        return false;
                    }

                    if (notifiedThread.get() != TobaccoProviderThread.this) {
                        retry();
                    }

                    paperAvailable.set(false);
                    matchesAvailable.set(false);
                    notifiedThread.set(null);
                    return true;
                }
            });
        }
    }
}



