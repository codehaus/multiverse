package org.multiverse.stms.beta;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.blocking.CheapLatch;

import static org.junit.Assert.*;

public class BetaObjectPool_listenersTest {
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        pool = new BetaObjectPool();
    }

    @Test(expected = NullPointerException.class)
    public void whenNullPutInPool_thenNullPointerException() {
        pool.putListeners(null);
    }

    @Test
    public void whenPutInPool_thenPreparedForPooling() {
        CheapLatch latch = new CheapLatch();
        latch.prepareForPooling();

        Listeners next = new Listeners();

        Listeners listeners = new Listeners();
        listeners.listener = latch;
        listeners.listenerEra = latch.getEra();
        listeners.next = next;

        pool.putListeners(listeners);

        assertNull(listeners.next);
        assertNull(listeners.listener);
        assertEquals(Long.MIN_VALUE, listeners.listenerEra);
    }

    @Test
    public void test() {
        Listeners listeners1 = new Listeners();
        Listeners listeners2 = new Listeners();
        Listeners listeners3 = new Listeners();

        pool.putListeners(listeners1);
        pool.putListeners(listeners2);
        pool.putListeners(listeners3);

        assertSame(listeners3, pool.takeListeners());
        assertSame(listeners2, pool.takeListeners());
        assertSame(listeners1, pool.takeListeners());
        assertNull(pool.takeListeners());
    }
}
