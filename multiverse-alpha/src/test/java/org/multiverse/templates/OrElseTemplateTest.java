package org.multiverse.templates;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.api.exceptions.RetryError;
import org.multiverse.transactional.primitives.TransactionalInteger;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

public class OrElseTemplateTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        setThreadLocalTransaction(null);
    }

    @After
    public void tearDown() {
        setThreadLocalTransaction(null);
    }

    public Transaction startUpdateTransaction() {
        Transaction t = stm.getTransactionFactoryBuilder()
                .setAutomaticReadTracking(true)
                .setReadonly(false)
                .build().start();
        setThreadLocalTransaction(t);
        return t;
    }

    @Test
    public void changesInLeftBranchAreNotUnsetWhenTheRightBranchIsEntered() {
        final TransactionalInteger leftRef = new TransactionalInteger();

        Transaction tx = startUpdateTransaction();
        new OrElseTemplate(tx){
            @Override
            public Object run(Transaction tx) {
                leftRef.set(1);
                retry();
                return null;
            }

            @Override
            public Object orelserun(Transaction tx) {
                assertEquals(1, leftRef.get());
                return null;
            }
        }.execute();

        assertEquals(1, leftRef.get());
    }

    @Test
    public void testWaitOnLeftBranch() {
        TransactionalInteger orRef = new TransactionalInteger();
        TransactionalInteger elseRef = new TransactionalInteger();

        WThread thread = new WThread(orRef, elseRef);
        thread.start();

        sleepMs(300);

        assertAlive(thread);

        orRef.inc();

        joinAll(thread);
        assertEquals("run", thread.result);
    }

    @Test
    public void testWaitOnRightBranch() {
        TransactionalInteger orRef = new TransactionalInteger();
        TransactionalInteger elseRef = new TransactionalInteger();

        WThread thread = new WThread(orRef, elseRef);
        thread.start();

        sleepMs(300);

        assertAlive(thread);

        elseRef.inc();

        joinAll(thread);
        assertEquals("orelserun", thread.result);
    }


    class WThread extends TestThread {
        private final TransactionalInteger orRef;
        private final TransactionalInteger elseRef;
        private String result;

        WThread(TransactionalInteger orRef, TransactionalInteger elseRef) {
            super("WaitThread");
            this.orRef = orRef;
            this.elseRef = elseRef;
        }

        @Override
        public void doRun() throws Exception {
            TransactionFactory txFactory = stm.getTransactionFactoryBuilder()
                    .setReadonly(false)
                    .setAutomaticReadTracking(true).build();

            result = new TransactionTemplate<String>(txFactory) {
                @Override
                public String execute(Transaction t) throws Exception {
                    return new OrElseTemplate<String>() {
                        @Override
                        public String run(Transaction tx) {
                            if (orRef.get() == 0) {
                                retry();
                            }

                            return "run";
                        }

                        @Override
                        public String orelserun(Transaction tx) {
                            if (elseRef.get() == 0) {
                                retry();
                            }

                            return "orelserun";
                        }
                    }.execute();
                }
            }.execute();
        }
    }

    @Test
    public void testThreadLocalTx() {
        final Transaction startedTx = startUpdateTransaction();

        new OrElseTemplate() {
            @Override
            public Object run(Transaction tx) {
                assertSame(startedTx, tx);
                retry();
                return null;
            }

            @Override
            public Object orelserun(Transaction tx) {
                assertSame(startedTx, tx);
                return null;
            }
        }.execute();

        assertSame(startedTx, getThreadLocalTransaction());
    }


    @Test
    public void testRunWasSuccess() {
        final TransactionalInteger v = new TransactionalInteger(0);

        Transaction t = startUpdateTransaction();

        new OrElseTemplate() {
            @Override
            public Object run(Transaction t) {
                v.set(10);
                return null;
            }

            @Override
            public Object orelserun(Transaction t) {
                fail();
                return null;
            }
        }.execute();

        t.commit();
        setThreadLocalTransaction(null);
        assertEquals(10, v.get());
    }

    @Test
    public void testRunWasFailureTryOrElseRun() {
        final TransactionalInteger v = new TransactionalInteger(0);

        Transaction t = startUpdateTransaction();

        new OrElseTemplate() {
            @Override
            public Object run(Transaction t) {
                v.set(10);
                retry();
                return null;
            }

            @Override
            public Object orelserun(Transaction t) {
                v.set(20);
                return null;
            }
        }.execute();

        t.commit();
        setThreadLocalTransaction(null);
        assertEquals(20, v.get());
    }

    @Test
    public void testRunWasFailureTryOrElseRunWasAlsoFailure() {
        final TransactionalInteger v = new TransactionalInteger(0);

        Transaction t = startUpdateTransaction();

        try {
            new OrElseTemplate() {
                @Override
                public Object run(Transaction t) {
                    v.set(10);
                    retry();
                    return null;
                }

                @Override
                public Object orelserun(Transaction t) {
                    v.set(20);
                    retry();
                    return null;
                }
            }.execute();
            fail();
        } catch (RetryError e) {
        }

        t.abort();
        setThreadLocalTransaction(null);

        assertIsAborted(t);
        assertEquals(0, v.get());
    }
}
