package org.multiverse.templates;

import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import org.multiverse.api.Stm;
import static org.multiverse.api.StmUtils.retry;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.RetryError;
import org.multiverse.datastructures.refs.IntRef;
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
        Transaction t = stm.startUpdateTransaction(null);
        setThreadLocalTransaction(t);
        return t;
    }

    @Test
    public void testRunWasSuccess() {
        final IntRef v = new IntRef(0);

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
        final IntRef v = new IntRef(0);

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
        final IntRef v = new IntRef(0);

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
