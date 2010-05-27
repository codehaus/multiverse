package org.multiverse.stms.alpha.instrumentation.integrationtest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.ThreadLocalTransaction;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.AlphaTransactionalObject;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.templates.TransactionTemplate;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.instrumentation.InstrumentationTestUtils.assertNoInstrumentationProblems;
import static org.multiverse.instrumentation.InstrumentationTestUtils.resetInstrumentationProblemMonitor;
import static org.multiverse.stms.alpha.instrumentation.AlphaReflectionUtils.*;

/**
 * @author Peter Veentjer
 */
public class StackTest {

    private AlphaStm stm;
    private TransactionFactory updateTxFactory;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
        updateTxFactory = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setReadonly(false)
                .build();
        resetInstrumentationProblemMonitor();
        clearThreadLocalTransaction();
    }

    @After
    public void tearDown() {
        assertNoInstrumentationProblems();
    }

    @Test
    public void testStructuralContent() {
        assertFalse(existsField(Stack.class, "size"));
        assertFalse(existsField(Stack.class, "head"));
        assertTrue(existsTranlocalClass(Stack.class));
        assertTrue(existsTranlocalField(Stack.class, "size"));
        assertTrue(existsTranlocalField(Stack.class, "head"));
    }

    @Test
    public void testIsTransformed() {
        Stack stack = new Stack();
        assertTrue(((Object) stack) instanceof AlphaTransactionalObject);
    }

    @Test
    public void readUncommitted() {
        new TransactionTemplate() {
            @Override
            public Object execute(Transaction t) throws Exception {
                Stack stack = new Stack();
                AlphaTransaction alphaTransaction = (AlphaTransaction) t;
                AlphaTranlocal tranlocal = alphaTransaction.openForWrite((AlphaTransactionalObject) ((Object) stack));
                assertFalse(tranlocal.isCommitted());
                assertSame(stack, tranlocal.getTransactionalObject());
                return null;
            }
        }.execute();
    }

    @Test
    public void readCommitted() {
        final Stack stack = new Stack();
        stack.push("foo");

        new TransactionTemplate() {
            @Override
            public Object execute(Transaction t) throws Exception {
                AlphaTransaction alphaTransaction = (AlphaTransaction) t;
                AlphaTranlocal tranlocal = alphaTransaction.openForWrite((AlphaTransactionalObject) ((Object) stack));
                assertFalse(tranlocal.isCommitted());
                assertSame(stack, tranlocal.getTransactionalObject());
                return null;
            }
        }.execute();
    }

    @Test
    public void readReadonly() {
        final Stack stack = new Stack();

        AlphaTransactionalObject transactionalObject = ((AlphaTransactionalObject) ((Object) stack));
        final AlphaTranlocal storedTranlocal = transactionalObject.___load();

        TransactionFactory txFactory = stm.getTransactionFactoryBuilder()
                .setReadonly(true).build();

        new TransactionTemplate(txFactory) {
            @Override
            public Object execute(Transaction t) throws Exception {
                AlphaTransaction alphaTransaction = (AlphaTransaction) t;
                AlphaTranlocal tranlocal = alphaTransaction.openForRead((AlphaTransactionalObject) ((Object) stack));
                assertEquals(stm.getVersion(), tranlocal.getWriteVersion());
                assertSame(storedTranlocal, tranlocal);
                assertSame(stack, tranlocal.getTransactionalObject());
                return null;
            }
        }.execute();
    }

    @Test
    public void testCreation() {
        long version = stm.getVersion();

        Stack stack = new Stack();

        assertEquals(version, stm.getVersion());
        assertTrue(stack.isEmpty());
        assertEquals(0, stack.size());
    }

    @Test
    public void testPush() {
        Stack<Integer> stack = new Stack<Integer>();

        long version = stm.getVersion();
        stack.push(1);

        assertEquals(version + 1, stm.getVersion());
        assertFalse(stack.isEmpty());
        assertEquals(1, stack.size());
    }

    @Test
    public void popFromNonEmptyStack() {
        Stack<Integer> stack = new Stack<Integer>();
        stack.push(10);

        long version = stm.getVersion();
        int result = stack.pop();
        assertEquals(version + 1, stm.getVersion());
        assertEquals(10, result);
        assertEquals(0, stack.size());
    }

    @Test
    public void clearOfNonEmptyStack() {
        Stack<Integer> stack = new Stack<Integer>();
        stack.push(10);

        long version = stm.getVersion();
        stack.clear();
        assertEquals(version + 1, stm.getVersion());
        assertEquals(0, stack.size());
    }

    @Test
    public void clearOfEmptyStack() {
        Stack<Integer> stack = new Stack<Integer>();

        long version = stm.getVersion();
        stack.clear();
        assertEquals(version, stm.getVersion());
        assertTrue(stack.isEmpty());
    }

    @Test
    public void testRollback() {
        Stack<String> stack = new Stack<String>();

        long version = stm.getVersion();

        Transaction t = updateTxFactory.start();
        ThreadLocalTransaction.setThreadLocalTransaction(t);

        stack.push("foo");
        stack.push("bar");

        t.abort();

        assertEquals(version, stm.getVersion());
        assertTrue(stack.isEmpty());
    }
}
