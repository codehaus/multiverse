package org.multiverse.stms.beta;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.blocking.Latch;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class Listeners_openAllTest {
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        pool = new BetaObjectPool();
    }

    @Test
    public void test() {
        Latch latch1 = mock(Latch.class);
        Latch latch2 = mock(Latch.class);

        Listeners listeners = new Listeners();
        listeners.listener = latch1;
        listeners.listenerEra = 1;

        listeners.next = new Listeners();
        listeners.next.listener = latch2;
        listeners.next.listenerEra = 2;

        listeners.openAll(pool);

        verify(latch1).open(1);
        verify(latch2).open(2);
    }
}
