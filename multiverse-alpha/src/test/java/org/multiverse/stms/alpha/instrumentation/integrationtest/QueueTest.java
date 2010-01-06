package org.multiverse.stms.alpha.instrumentation.integrationtest;

import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.api.GlobalStmInstance.setGlobalStmInstance;
import org.multiverse.api.ThreadLocalTransaction;
import org.multiverse.api.Transaction;
import org.multiverse.stms.alpha.AlphaAtomicObject;
import org.multiverse.stms.alpha.AlphaStm;
import static org.multiverse.stms.alpha.instrumentation.AlphaReflectionUtils.*;

/**
 * @author Peter Veentjer
 */
public class QueueTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = new AlphaStm();
        setGlobalStmInstance(stm);
    }

    @After
    public void tearDown() {
        //assertNoInstrumentationProblems();
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
    public void testIsNotTransformedToAlphaAtomicObject() {
        Queue queue = new Queue();

        assertFalse(((Object) queue) instanceof AlphaAtomicObject);
    }

    @Test
    public void testConstruction() {
        long version = stm.getTime();
        Queue queue = new Queue(100);

        assertEquals(version + 1, stm.getTime());
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

        long version = stm.getTime();

        Transaction t = stm.startUpdateTransaction("testRollback");
        ThreadLocalTransaction.setThreadLocalTransaction(t);

        queue.push("foo");
        queue.push("bar");

        t.abort();

        assertEquals(version, stm.getTime());
        assertTrue(queue.isEmpty());
    }
}
