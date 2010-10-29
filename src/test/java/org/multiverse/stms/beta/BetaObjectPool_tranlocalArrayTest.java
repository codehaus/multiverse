package org.multiverse.stms.beta;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.transactionalobjects.BetaTranlocal;

import static org.junit.Assert.*;
import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;

public class BetaObjectPool_tranlocalArrayTest {
    private BetaObjectPool pool;
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    public void whenItemPutInPool_thenPreparedForPooling() {
        BetaTranlocal[] array = new BetaTranlocal[2];
        array[0] = newLongRef(stm).___newTranlocal();
        array[1] = newLongRef(stm).___newTranlocal();

        pool.putTranlocalArray(array);

        assertNull(array[0]);
        assertNull(array[1]);
    }

    @Test(expected = NullPointerException.class)
    public void whenNullArrayAdded_thenNullPointerException() {
        pool.putTranlocalArray(null);
    }

    @Test
    public void normalScenario_0() {
        normalScenario(0);
    }

    @Test
    public void normalScenario_1() {
        normalScenario(1);
    }

    @Test
    public void normalScenario_10() {
        normalScenario(10);
    }

    @Test
    public void normalScenario_100() {
        normalScenario(100);
    }

    public void normalScenario(int size) {
        BetaTranlocal[] array = new BetaTranlocal[size];
        pool.putTranlocalArray(array);

        BetaTranlocal[] result = pool.takeTranlocalArray(array.length);
        assertSame(array, result);

        BetaTranlocal[] result2 = pool.takeTranlocalArray(array.length);
        assertNotNull(result2);
        assertNotSame(result, result2);
    }

}
