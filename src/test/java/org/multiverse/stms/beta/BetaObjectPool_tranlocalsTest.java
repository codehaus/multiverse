package org.multiverse.stms.beta;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.refs.LongRef;
import org.multiverse.stms.beta.refs.LongRefTranlocal;
import org.multiverse.stms.beta.refs.Tranlocal;

import static org.junit.Assert.*;

/**
 * @author Peter Veentjer
 */
public class BetaObjectPool_tranlocalsTest {
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        pool = new BetaObjectPool();
    }

    @Test
    public void putUpdate() {
        LongRef ref = new LongRef(0);
        LongRefTranlocal put = new LongRefTranlocal(ref);
        pool.put(put);

        assertNull(put.owner);
        assertNull(put.read);
        assertEquals(0, put.value);
        assertFalse(put.isCommitted);
        assertFalse(put.isPermanent);
    }

    @Test
    public void putCommitted() {
        LongRef ref = new LongRef(0);
        LongRefTranlocal put = new LongRefTranlocal(ref);
        put.isCommitted = true;
        pool.put(put);

        assertNull(put.owner);
        assertNull(put.read);
        assertEquals(0, put.value);
        assertFalse(put.isCommitted);
        assertFalse(put.isPermanent);
    }

    @Test
    public void take() {
        LongRef ref = new LongRef(0);
        LongRefTranlocal put = new LongRefTranlocal(ref);
        pool.put(put);

        Tranlocal result = pool.take(ref);

        assertSame(put, result);
        assertSame(ref, put.owner);
        assertNull(put.read);
        assertEquals(0, put.value);
        assertFalse(put.isCommitted);
        assertFalse(put.isPermanent);
    }

    @Test
    public void takeFromEmptyPool() {
        LongRef ref = new LongRef(0);

        Tranlocal result = pool.take(ref);
        assertNull(result);
    }

    @Test
    public void test() {
        LongRef ref = new LongRef(0);
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
        assertNull(take5);
    }
}
