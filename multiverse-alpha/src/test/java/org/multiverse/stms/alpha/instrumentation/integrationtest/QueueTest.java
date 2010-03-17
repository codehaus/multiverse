package org.multiverse.stms.alpha.instrumentation.integrationtest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.ThreadLocalTransaction;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.javaagent.JavaAgentProblemMonitor;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaTransactionalObject;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.instrumentation.InstrumentationTestUtils.resetInstrumentationProblemMonitor;
import static org.multiverse.stms.alpha.instrumentation.AlphaReflectionUtils.*;

/**
 * @author Peter Veentjer
 */
public class QueueTest {

    private AlphaStm stm;
    private TransactionFactory updateTxFactory;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
        updateTxFactory = stm.getTransactionFactoryBuilder().build();
        resetInstrumentationProblemMonitor();
    }

    @After
    public void tearDown() {
        assertFalse(JavaAgentProblemMonitor.INSTANCE.isProblemFound());
    }

    @Test
    public void testStructuralContent() {
        assertTrue(existsField(Queue.class, "pushedStack"));
        assertTrue(existsField(Queue.class, "readyToPopStack"));
        assertTrue(existsField(Queue.class, "maxCapacity"));
        assertFalse(existsTranlocalClass(Queue.class));
        assertFalse(existsTranlocalSnapshotClass(Queue.class));
    }

    @Test
    public void testIsNotTransformedToAlphaTransactionalObject() {
        Queue queue = new Queue();

        assertFalse(((Object) queue) instanceof AlphaTransactionalObject);
    }

    @Test
    public void testConstruction() {
        long version = stm.getVersion();
        Queue queue = new Queue(100);

        assertEquals(version + 1, stm.getVersion());
        assertEquals(0, queue.size());
        assertTrue(queue.isEmpty());
    }

    @Test
    public void complexScenario() {
        Queue<String> queue = new Queue<String>(100);
        queue.push("1");
        queue.push("2");

        assertEquals("1", queue.take());

        queue.push("3");

        assertEquals("2", queue.take());
        assertEquals("3", queue.take());
    }

    @Test
    public void testRollback() {
        Queue<String> queue = new Queue<String>();

        long version = stm.getVersion();

        Transaction t = updateTxFactory.start();
        ThreadLocalTransaction.setThreadLocalTransaction(t);

        queue.push("foo");
        queue.push("bar");

        t.abort();

        assertEquals(version, stm.getVersion());
        assertTrue(queue.isEmpty());
    }
}
