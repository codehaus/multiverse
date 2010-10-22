package org.multiverse.stms.beta;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.LongRefTranlocal;
import org.multiverse.stms.beta.transactionalobjects.Tranlocal;

import static org.junit.Assert.*;

/**
 * @author Peter Veentjer
 */
public class BetaObjectPool_tranlocalsTest implements BetaStmConstants{
    private BetaObjectPool pool;
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test(expected = NullPointerException.class)
    public void putNullInPoolInSpecializedPool_thenNullPointerException() {
        pool.put((LongRefTranlocal) null);
    }

    @Test
    public void putUpdate() {
        BetaLongRef ref = new BetaLongRef(stm.startDefaultTransaction());
        LongRefTranlocal put = new LongRefTranlocal(ref);
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
        LongRefTranlocal put = new LongRefTranlocal(ref);
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
        LongRefTranlocal put = new LongRefTranlocal(ref);
        pool.put(put);

        Tranlocal result = pool.take(ref);

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

        Tranlocal result = pool.take(ref);
        assertNotNull(result);
    }

    @Test
    public void test() {
        BetaLongRef ref = new BetaLongRef(stm.startDefaultTransaction());
        LongRefTranlocal put1 = new LongRefTranlocal(ref);
        LongRefTranlocal put2 = new LongRefTranlocal(ref);
        LongRefTranlocal put3 = new LongRefTranlocal(ref);
        LongRefTranlocal put4 = new LongRefTranlocal(ref);

        pool.put(put1);
        pool.put(put2);
        pool.put(put3);
        pool.put(put4);

        Tranlocal take1 = pool.take(ref);
        Tranlocal take2 = pool.take(ref);
        Tranlocal take3 = pool.take(ref);
        Tranlocal take4 = pool.take(ref);
        Tranlocal take5 = pool.take(ref);

        assertSame(take1, put4);
        assertSame(take2, put3);
        assertSame(take3, put2);
        assertSame(take4, put1);
        assertNotNull(take5);
    }
}
