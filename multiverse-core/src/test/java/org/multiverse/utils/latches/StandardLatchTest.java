package org.multiverse.utils.latches;

import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Test;
import org.multiverse.TestThread;
import static org.multiverse.TestUtils.*;

import java.util.concurrent.TimeUnit;

/**
 * Created by IntelliJ IDEA. User: alarmnummer Date: Oct 29, 2009 Time: 7:43:06 PM To change this template use File |
 * Settings | File Templates.
 */
public class StandardLatchTest {

    @After
    public void tearDown() {
        //clear the interrupted status
        Thread.interrupted();
    }

    @Test
    public void noArgConstructor() {
        StandardLatch latch = new StandardLatch();
        assertFalse(latch.isOpen());
    }

    @Test
    public void singleArgConstructor() {
        assertTrue(new StandardLatch(true).isOpen());
        assertFalse(new StandardLatch(false).isOpen());
    }

    @Test
    public void openLatchThatAlreadyIsOpen() {
        StandardLatch latch = new StandardLatch(true);
        latch.open();

        assertTrue(latch.isOpen());
    }

    // ====================== await ============================

    @Test
    public void awaitOnOpenLatch() throws InterruptedException {
        StandardLatch latch = new StandardLatch(true);
        latch.await();
        assertTrue(latch.isOpen());
    }

    @Test
    public void awaitOnOpenLatchWithInterruptedStatus() throws InterruptedException {
        Thread.currentThread().interrupt();

        StandardLatch latch = new StandardLatch(true);
        latch.await();
        assertTrue(latch.isOpen());
        assertIsInterrupted(Thread.currentThread());
    }

    @Test
    public void await() {
        StandardLatch latch = new StandardLatch();

        AwaitThread awaitThread1 = new AwaitThread(latch);
        AwaitThread awaitThread2 = new AwaitThread(latch);
        startAll(awaitThread1, awaitThread2);

        sleepMs(100);
        assertAreAlive(awaitThread1, awaitThread2);

        latch.open();
        joinAll(awaitThread1, awaitThread2);

        assertTrue(latch.isOpen());
    }

    // ====================== awaitUninterruptible ============================

    @Test
    public void awaitOnUninterruptibleOpenLatch() throws InterruptedException {
        StandardLatch latch = new StandardLatch(true);
        latch.awaitUninterruptible();
        assertTrue(latch.isOpen());
    }

    @Test
    public void awaitOnUninterruptibleOpenLatchWithInterruptedStatus() throws InterruptedException {
        Thread.currentThread().interrupt();

        StandardLatch latch = new StandardLatch(true);
        latch.awaitUninterruptible();
        assertTrue(latch.isOpen());
        assertIsInterrupted(Thread.currentThread());
    }

    @Test
    public void awaitUninterruptible() {
        StandardLatch latch = new StandardLatch();

        AwaitUninterruptibleThread awaitThread1 = new AwaitUninterruptibleThread(latch);
        AwaitUninterruptibleThread awaitThread2 = new AwaitUninterruptibleThread(latch);
        startAll(awaitThread1, awaitThread2);

        sleepMs(100);
        assertAreAlive(awaitThread1, awaitThread2);

        latch.open();
        joinAll(awaitThread1, awaitThread2);

        assertTrue(latch.isOpen());
    }

    // ============================ tryAwait ========================

    @Test(expected = NullPointerException.class)
    public void tryAwaitFailsWithNullUnit() throws InterruptedException {
        StandardLatch latch = new StandardLatch();
        latch.tryAwait(1, null);
    }

    @Test
    public void tryAwaitOnOpenLatch() throws InterruptedException {
        StandardLatch latch = new StandardLatch(true);

        boolean result = latch.tryAwait(1, TimeUnit.MILLISECONDS);
        assertTrue(result);
    }

    @Test
    public void tryAwaitOnClosedLatchAndTimeout() throws InterruptedException {
        StandardLatch latch = new StandardLatch(false);

        boolean result = latch.tryAwait(1, TimeUnit.MILLISECONDS);
        assertFalse(result);
    }

    // ============================= tryAwaitUninteruptible ================

    @Test
    public void tryAwaitUninterruptibleOnOpenLatch(){
        StandardLatch latch = new StandardLatch(true);
        boolean result = latch.tryAwaitUninterruptible(1, TimeUnit.NANOSECONDS);
        assertTrue(result);
    }

    @Test
    public void tryAwaitUninterruptibleWithNullUnit() {
        StandardLatch latch = new StandardLatch();
        try {
            latch.tryAwaitUninterruptible(1, null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertFalse(latch.isOpen());
    }

    @Test
    public void tryAwaitUninterruptibleWithTimeout() {
        StandardLatch latch = new StandardLatch();
        boolean result = latch.tryAwaitUninterruptible(100, TimeUnit.MILLISECONDS);
        assertFalse(result);
        assertFalse(latch.isOpen());
    }

    @Test
    public void tryAwaitUninterruptibleWithInterruptStatus() {
        StandardLatch latch = new StandardLatch();
        Thread.currentThread().interrupt();

        boolean result = latch.tryAwaitUninterruptible(100, TimeUnit.MILLISECONDS);
        assertFalse(result);
        assertFalse(latch.isOpen());
        assertIsInterrupted(Thread.currentThread());
    }

    @Test
    public void tryAwaitUninterruptible() {
        StandardLatch latch = new StandardLatch();

        TryAwaitUninterruptibleThread t = new TryAwaitUninterruptibleThread(latch, 500, TimeUnit.MILLISECONDS);
        t.start();

        sleepMs(50);
        assertNull(t.result);
        assertTrue(t.isAlive());

        latch.open();
        joinAll(t);

        assertTrue(t.result);
        assertTrue(latch.isOpen());
    }

    @Test
    public void tryAwaitUninterruptibleInterruptedWhileWaiting() {
        StandardLatch latch = new StandardLatch();

        TryAwaitUninterruptibleThread t = new TryAwaitUninterruptibleThread(latch, 500, TimeUnit.MILLISECONDS);
        startAll(t);

        sleepMs(50);
        t.interrupt();
        assertNull(t.result);
        assertTrue(t.isAlive());

        latch.open();
        joinAll(t);

        assertTrue(t.hasEndedWithInterruptStatus());
        assertTrue(t.result);
        assertTrue(latch.isOpen());
    }

    class TryAwaitUninterruptibleThread extends TestThread {
        private final Latch latch;
        private long timeout;
        private TimeUnit unit;
        private volatile Boolean result;

        TryAwaitUninterruptibleThread(Latch latch, long timeout, TimeUnit unit) {
            super("TryAwaitUninterruptibleThread");
            this.latch = latch;
            this.timeout = timeout;
            this.unit = unit;
        }

        @Override
        public void doRun() throws Exception {
            result = latch.tryAwaitUninterruptible(timeout, unit);
        }
    }

    @Test
    public void testToString() {
        assertEquals("StandardLatch(open=false)", new StandardLatch(false).toString());
        assertEquals("StandardLatch(open=true)", new StandardLatch(true).toString());
    }
}
