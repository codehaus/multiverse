package org.multiverse.api;

import org.junit.Test;
import org.multiverse.api.latches.CheapLatch;
import org.multiverse.api.latches.Latch;

import static org.junit.Assert.*;

/**
 * @author Peter Veentjer
 */
public class ListenersTest {

    @Test
    public void getNext() {
        Latch latch1 = new CheapLatch();
        Latch latch2 = new CheapLatch();

        Listeners tail = new Listeners(latch2, null);
        Listeners head = new Listeners(latch1, tail);

        assertSame(tail, head.getNext());
        assertNull(tail.getNext());
    }

    @Test
    public void testOpenAllOnClosedListeners() {
        Latch latch1 = new CheapLatch();
        Latch latch2 = new CheapLatch();

        Listeners listeners = new Listeners(latch1, new Listeners(latch2, null));
        listeners.openAll();

        assertTrue(latch1.isOpen());
        assertTrue(latch2.isOpen());
    }

    @Test
    public void testOpenAllOnOpenListeners() {
        Latch latch1 = new CheapLatch(true);
        Latch latch2 = new CheapLatch(true);

        Listeners listeners = new Listeners(latch1, new Listeners(latch2, null));
        listeners.openAll();

        assertTrue(latch1.isOpen());
        assertTrue(latch2.isOpen());
    }
}
