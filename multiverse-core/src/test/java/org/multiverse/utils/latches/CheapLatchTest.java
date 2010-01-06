package org.multiverse.utils.latches;

import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Test;
import static org.multiverse.TestUtils.*;

import java.util.concurrent.TimeUnit;

/**
 * @author Peter Veentjer
 */
public class CheapLatchTest {

    @After
    public void tearDown(){
        Thread.interrupted();//clear the interrupt status
    }

    @Test
    public void test_OPEN_LATCH() {
        assertTrue(CheapLatch.OPEN_LATCH.isOpen());
    }

    @Test
    public void constructorWithOpenLatch() {
        CheapLatch latch = new CheapLatch(true);
        assertTrue(latch.isOpen());
    }

    @Test
    public void constructorWithClosedLatch() {
        CheapLatch latch = new CheapLatch(false);
        assertFalse(latch.isOpen());
    }

    @Test
    public void constructorWithNoArgs() {
        CheapLatch latch = new CheapLatch();
        assertFalse(latch.isOpen());
    }

    @Test
    public void openAlreadyOpenLatch() {
        CheapLatch latch = new CheapLatch(true);
        latch.open();
        assertTrue(latch.isOpen());
    }

    @Test
    public void awaitOpenLatchCompletes() throws InterruptedException {
        CheapLatch latch = new CheapLatch(true);
        latch.await();

        assertTrue(latch.isOpen());
    }

    @Test
    public void awaitClosedLatchIsInterruptedIfStartingWithInterruptedFlag() throws InterruptedException {
        CheapLatch latch = new CheapLatch();

        AwaitThread awaitThread = new AwaitThread(latch,true);

        awaitThread.start();

        awaitThread.join();
        assertTrue(awaitThread.getThrowable() instanceof InterruptedException);
    }

    @Test
    public void awaitClosedLatchCompletesWhenLatchIsOpened() throws InterruptedException {
        CheapLatch latch = new CheapLatch();

        AwaitThread awaitThread1 = new AwaitThread(latch);
        AwaitThread awaitThread2 = new AwaitThread(latch);

        startAll(awaitThread1, awaitThread2);
        sleepMs(500);
        assertTrue(awaitThread1.isAlive());
        assertTrue(awaitThread2.isAlive());

        latch.open();
        joinAll(awaitThread1, awaitThread2);
    }


    @Test
    public void awaitUninterruptibleCompletesWhenLatchIsOpen() {
        CheapLatch latch = new CheapLatch(true);
        AwaitUninterruptibleThread awaitThread = new AwaitUninterruptibleThread(latch, false);
        startAll(awaitThread);
        joinAll(awaitThread);
    }

    @Test
    public void awaitUninterruptibleCompletesIsOpened() {
        CheapLatch latch = new CheapLatch();

        AwaitUninterruptibleThread awaitThread1 = new AwaitUninterruptibleThread(latch, false);
        AwaitUninterruptibleThread awaitThread2 = new AwaitUninterruptibleThread(latch, true);

        startAll(awaitThread1, awaitThread2);
        sleepMs(500);
        assertTrue(awaitThread1.isAlive());
        assertTrue(awaitThread2.isAlive());

        latch.open();
        joinAll(awaitThread1, awaitThread2);

        assertFalse(awaitThread1.hasEndedWithInterruptStatus());
        assertTrue(awaitThread2.hasEndedWithInterruptStatus());
    }

    @Test
    public void tryAwaitWithTimeoutFails() throws InterruptedException {
        CheapLatch latch = new CheapLatch(false);

        try {
            latch.tryAwait(10, TimeUnit.SECONDS);
            fail();
        } catch (UnsupportedOperationException expected) {
        }

        assertFalse(latch.isOpen());
    }

    @Test
    public void tryAwaitUninterruptibleFails(){
        CheapLatch latch = new CheapLatch(false);

        try {
            latch.tryAwaitUninterruptible(10, TimeUnit.SECONDS);
            fail();
        } catch (UnsupportedOperationException expected) {
        }

        assertFalse(latch.isOpen());
    }

    @Test
    public void testToString() {
        CheapLatch latch = new CheapLatch();
        assertEquals("CheapLatch(open=false)", latch.toString());
        latch.open();
        assertEquals("CheapLatch(open=true)", latch.toString());
    }
}
