package org.multiverse.stms.gamma;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.gamma.transactionalobjects.GammaLongRef;
import org.multiverse.stms.gamma.transactionalobjects.GammaTranlocal;
import org.multiverse.stms.gamma.transactions.GammaTransaction;

import static org.junit.Assert.*;

public class GammaObjectPool_tranlocalArrayTest implements GammaConstants{
    private GammaObjectPool pool;
    private GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
        pool = new GammaObjectPool();
    }

    @Test
    public void whenItemPutInPool_thenPreparedForPooling() {
        GammaTranlocal[] array = new GammaTranlocal[2];
        GammaTransaction tx = stm.startDefaultTransaction();

        array[0] = new GammaLongRef(stm).openForRead(tx, LOCKMODE_NONE);
        array[1] = new GammaLongRef(stm).openForRead(tx, LOCKMODE_NONE);

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
        GammaTranlocal[] array = new GammaTranlocal[size];
        pool.putTranlocalArray(array);

        GammaTranlocal[] result = pool.takeTranlocalArray(array.length);
        assertSame(array, result);

        GammaTranlocal[] result2 = pool.takeTranlocalArray(array.length);
        assertNotNull(result2);
        assertNotSame(result, result2);
    }
}
