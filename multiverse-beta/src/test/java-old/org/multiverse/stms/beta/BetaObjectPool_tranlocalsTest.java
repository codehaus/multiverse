package org.multiverse.stms.beta;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRefTranlocal;
import org.multiverse.stms.beta.transactionalobjects.BetaTranlocal;

import static org.junit.Assert.*;

/**
 * @author Peter Veentjer
 */
public class BetaObjectPool_tranlocalsTest implements BetaStmConstants {
    private BetaObjectPool pool;
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test(expected = NullPointerException.class)
    public void putNullInPoolInSpecializedPool_thenNullPointerException() {
        pool.put((BetaLongRefTranlocal) null);
    }

    @Test
    public void putUpdate() {
        BetaLongRef ref = new BetaLongRef(stm.startDefaultTransaction());
        BetaLongRefTranlocal put = new BetaLongRefTranlocal(ref);
        pool.put(put);

        assertNull(put.owner);
        //todo:
        //assertNull(put.read);
        assertEquals(0, put.value);
        assertFalse(put.isReadonly());
        assertFalse(put.hasDepartObligation());
    }

    @Test
    public void putReadonly() {
        BetaLongRef ref = new BetaLongRef(stm.startDefaultTransaction());
        BetaLongRefTranlocal put = new BetaLongRefTranlocal(ref);
        put.setStatus(STATUS_READONLY);
        pool.put(put);

        assertNull(put.owner);

        //todo:
        // assertNull(put.read);
        assertEquals(0, put.value);
        assertFalse(put.isReadonly());
        assertFalse(put.hasDepartObligation());
    }

    @Test
    public void take() {
        BetaLongRef ref = new BetaLongRef(stm.startDefaultTransaction());
        BetaLongRefTranlocal put = new BetaLongRefTranlocal(ref);
        pool.put(put);

        BetaTranlocal result = pool.take(ref);

        assertSame(put, result);
        assertSame(ref, put.owner);
        //todo:
        //assertNull(put.read);
        assertEquals(0, put.value);
        assertFalse(put.isReadonly());
        assertFalse(put.hasDepartObligation());
    }

    @Test
    public void takeFromEmptyPool() {
        BetaLongRef ref = new BetaLongRef(stm.startDefaultTransaction());

        BetaTranlocal result = pool.take(ref);
        assertNotNull(result);
    }

    @Test
    public void test() {
        BetaLongRef ref = new BetaLongRef(stm.startDefaultTransaction());
        BetaLongRefTranlocal put1 = new BetaLongRefTranlocal(ref);
        BetaLongRefTranlocal put2 = new BetaLongRefTranlocal(ref);
        BetaLongRefTranlocal put3 = new BetaLongRefTranlocal(ref);
        BetaLongRefTranlocal put4 = new BetaLongRefTranlocal(ref);

        pool.put(put1);
        pool.put(put2);
        pool.put(put3);
        pool.put(put4);

        BetaTranlocal take1 = pool.take(ref);
        BetaTranlocal take2 = pool.take(ref);
        BetaTranlocal take3 = pool.take(ref);
        BetaTranlocal take4 = pool.take(ref);
        BetaTranlocal take5 = pool.take(ref);

        assertSame(take1, put4);
        assertSame(take2, put3);
        assertSame(take3, put2);
        assertSame(take4, put1);
        assertNotNull(take5);
    }
}
