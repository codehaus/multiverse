package org.multiverse.stms.beta;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.transactionalobjects.Tranlocal;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.multiverse.stms.beta.BetaStmUtils.newLongRef;

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
        Tranlocal[] array = new Tranlocal[2];
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
        Tranlocal[] array = new Tranlocal[size];
        pool.putTranlocalArray(array);

        Tranlocal[] result = pool.takeTranlocalArray(array.length);
        assertSame(array, result);

        Tranlocal[] result2 = pool.takeTranlocalArray(array.length);
        assertNull(result2);
    }

}
