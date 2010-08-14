package org.multiverse.stms.beta.refs;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.functions.LongFunction;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;

public class LongRefTranlocal_prepareForPoolingTest {

    private BetaObjectPool pool;
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    @Ignore
    public void whenConstructed() {

    }

    @Test
    @Ignore
    public void whenLocked() {

    }

    @Test
    @Ignore
    public void whenPermanent() {

    }

    @Test
    @Ignore
    public void whenNonPermanent() {

    }

    @Test
    public void whenUpdate() {
        LongRef ref = createLongRef(stm, 100);
        LongRefTranlocal tranlocal = ref.unsafeLoad().openForWrite(pool);

        tranlocal.prepareForPooling(pool);

        assertFalse(tranlocal.isPermanent);
        assertFalse(tranlocal.isDirty);
        assertEquals(0, tranlocal.value);
        assertFalse(tranlocal.isCommitted);
        assertFalse(tranlocal.isCommuting);
        assertNull(tranlocal.headCallable);
        assertNull(tranlocal.owner);
    }

    @Test
    public void whenCommuting() {
        LongRef ref = createLongRef(stm, 100);
        LongRefTranlocal tranlocal = ref.openForCommute(pool);

        LongFunction function = mock(LongFunction.class);
        tranlocal.addCommutingFunction(function, pool);

        tranlocal.prepareForPooling(pool);

        assertFalse(tranlocal.isPermanent);
        assertFalse(tranlocal.isDirty);
        assertEquals(0, tranlocal.value);
        assertFalse(tranlocal.isCommitted);
        assertFalse(tranlocal.isCommuting);
        assertNull(tranlocal.headCallable);
        assertNull(tranlocal.owner);
    }
}
