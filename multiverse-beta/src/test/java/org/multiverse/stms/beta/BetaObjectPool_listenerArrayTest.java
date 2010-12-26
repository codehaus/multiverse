package org.multiverse.stms.beta;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class BetaObjectPool_listenerArrayTest {
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        pool = new BetaObjectPool();
    }

    @Test(expected = NullPointerException.class)
    public void whenNullPutInPool_thenNullPointerException() {
        pool.putListenersArray(null);
    }
}
