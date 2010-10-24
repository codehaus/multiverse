package org.multiverse.stms.beta.integrationtest.classic;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.TestUtils;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicBooleanClosure;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.api.references.BooleanRef;
import org.multiverse.api.references.Ref;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.StmUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * http://en.wikipedia.org/wiki/Cigarette_smokers_problem
 */
public class CigaretteSmokersProblemTest {
    private static final int SMOKE_TIME_SECONDS = 10;

    private BooleanRef tobaccoAvailable;
    private BooleanRef paperAvailable;
    private BooleanRef matchesAvailable;
    private Ref<Thread> notifier;
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
        notifier = newRef();
        arbiterThread = new ArbiterThread();
        paperProvider = new PaperProviderThread();
        matchProvider = new MatchProviderThread();
        tobaccoProvider = new TobaccoProviderThread();
        stop = false;

        block = getGlobalStmInstance()
                .createTransactionFactoryBuilder()
                        //.setPessimisticLockLevel(PessimisticLockLevel.PrivatizeReads)
                        //.setIsolationLevel(IsolationLevel.Serializable)
                .buildAtomicBlock();
    }

    @Test
    public void test() {
        startAll(arbiterThread, paperProvider, matchProvider, tobaccoProvider);
        sleepMs(60000);
        System.out.println("Stopping threads");
        stop = true;
        joinAll(arbiterThread, paperProvider, matchProvider, tobaccoProvider);

        assertEquals(arbiterThread.count,
                paperProvider.count + matchProvider.count + tobaccoProvider.count);
    }

    class ArbiterThread extends TestThread {
        private int count;

        public ArbiterThread() {
            super("Arbiter");
        }

        @Override
        public void doRun() throws Exception {
            while (!stop) {
                count++;
                switch (TestUtils.randomInt(3)) {
                    case 0:
                        block.execute(new AtomicVoidClosure() {
                            @Override
                            public void execute(Transaction tx) {
                                if (notifier.get() != null) {
                                    retry();
                                }

                                tobaccoAvailable.set(true);
                                paperAvailable.set(true);
                                notifier.set(matchProvider);
                            }
                        });
                        break;
                    case 1:
                        block.execute(new AtomicVoidClosure() {
                            @Override
                            public void execute(Transaction tx) {
                                if (notifier.get() != null) {
                                    retry();
                                }

                                tobaccoAvailable.set(true);
                                matchesAvailable.set(true);
                                notifier.set(paperProvider);
                            }
                        });
                        break;
                    case 2:
                        block.execute(new AtomicVoidClosure() {
                            @Override
                            public void execute(Transaction tx) {
                                if (notifier.get() != null) {
                                    retry();
                                }

                                matchesAvailable.set(true);
                                paperAvailable.set(true);
                                notifier.set(tobaccoProvider);
                            }
                        });
                        break;
                    default:
                        throw new RuntimeException();
                }
            }
            notifier.atomicSet(arbiterThread);
        }
    }

    class PaperProviderThread extends TestThread {
        private int count;

        public PaperProviderThread() {
            super("PaperProvidingSmoker");
        }

        @Override
        public void doRun() throws Exception {
            while (makeCigarette()) {
                sleepRandomMs(SMOKE_TIME_SECONDS);
                count++;
                if (count % 100 == 0) {
                    System.out.printf("%s is at %s\n", getName(), count);
                }
            }
        }

        private boolean makeCigarette() {
            return block.execute(new AtomicBooleanClosure() {
                @Override
                public boolean execute(Transaction tx) throws Exception {
                    if (notifier.get() != PaperProviderThread.this) {
                        if (notifier.get() == arbiterThread) {
                            return false;
                        }

                        retry();
                    }

                    matchesAvailable.set(false);
                    tobaccoAvailable.set(false);
                    notifier.set(null);
                    return true;
                }
            });
        }
    }

    class MatchProviderThread extends TestThread {
        private int count;

        public MatchProviderThread() {
            super("MatchProvidingSmoker");
        }

        @Override
        public void doRun() throws Exception {

            while (makeCigarette()) {
                sleepRandomMs(SMOKE_TIME_SECONDS);
                count++;
                if (count % 100 == 0) {
                    System.out.printf("%s is at %s\n", getName(), count);
                }
            }
        }

        private boolean makeCigarette() {
            return block.execute(new AtomicBooleanClosure() {
                @Override
                public boolean execute(Transaction tx) throws Exception {
                    if (notifier.get() != MatchProviderThread.this) {
                        if (notifier.get() == arbiterThread) {
                            return false;
                        }
                        retry();
                    }

                    paperAvailable.set(false);
                    tobaccoAvailable.set(false);
                    notifier.set(null);
                    return true;
                }
            });
        }
    }

    class TobaccoProviderThread extends TestThread {
        private int count;

        public TobaccoProviderThread() {
            super("TobaccoProvidingSmoker");
        }

        @Override
        public void doRun() throws Exception {

            while (makeCigarette()) {
                sleepRandomMs(SMOKE_TIME_SECONDS);

                count++;
                if (count % 100 == 0) {
                    System.out.printf("%s is at %s\n", getName(), count);
                }
            }
        }

        private boolean makeCigarette() {
            return block.execute(new AtomicBooleanClosure() {
                @Override
                public boolean execute(Transaction tx) throws Exception {
                    if (notifier.get() != TobaccoProviderThread.this) {
                        if (notifier.get() == arbiterThread) {
                            return false;
                        }

                        retry();
                    }

                    paperAvailable.set(false);
                    matchesAvailable.set(false);
                    notifier.set(null);
                    return true;
                }
            });
        }
    }
}



