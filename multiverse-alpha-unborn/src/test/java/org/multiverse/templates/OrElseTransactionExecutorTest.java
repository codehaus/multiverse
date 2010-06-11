package org.multiverse.templates;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import static org.multiverse.api.StmUtils.retry;

import org.multiverse.api.exceptions.Retry;
import org.multiverse.transactional.refs.IntRef;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

public class OrElseTransactionExecutorTest {
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
                .setReadTrackingEnabled(true)
                .setReadonly(false)
                .build().start();
        setThreadLocalTransaction(t);
        return t;
    }
    @Test
    public void testUseEitherBlock() {
        final IntRef v = new IntRef(0);

        Transaction t = startUpdateTransaction();
        EitherCallable either = new EitherCallable(){
            @Override
            public Object call(Transaction tx) throws Exception {
                v.set(10);
                return null;
            }
        };
        OrElseCallable orelse = new OrElseCallable(){
            @Override
            public Object call(Transaction tx) throws Exception {
                fail("OrElse cannot be called");
                return null;
            }
        };
        try {
            new OrElseTransactionExecutor(t).execute(either, orelse);
        } catch (Exception e) {
            fail();
        } 

        t.commit();
        setThreadLocalTransaction(null);
        assertEquals(10, v.get());
    }
    @Test
    public void testUseOrElseBlock() {
        final IntRef v = new IntRef(0);

        Transaction t = startUpdateTransaction();
        EitherCallable either = new EitherCallable(){
            @Override
            public Object call(Transaction tx) throws Exception {
                retry();
                return null;
            }
        };
        OrElseCallable orelse = new OrElseCallable(){
            @Override
            public Object call(Transaction tx) throws Exception {
                v.set(10);
                return null;
            }
        };
        try {
            new OrElseTransactionExecutor(t).execute(either, orelse);
        } catch (Exception e) {
            fail();
        }

        t.commit();
        setThreadLocalTransaction(null);
        assertEquals(10, v.get());
    }
    @Test
    public void testNoChangeIfOrElseAlsoRetries() {
        final IntRef v = new IntRef(0);

        Transaction t = startUpdateTransaction();
        EitherCallable either = new EitherCallable(){
            @Override
            public Object call(Transaction tx) throws Exception {
                retry();
                return null;
            }
        };
        OrElseCallable orelse = new OrElseCallable(){
            @Override
            public Object call(Transaction tx) throws Exception {
                retry();
                return null;
            }
        };
        try {
            new OrElseTransactionExecutor(t).execute(either, orelse);
        } catch (Retry e) {
        } catch (Exception e) {
            fail();
        }

        t.commit();
        setThreadLocalTransaction(null);
        assertEquals(0, v.get());
    }
}
