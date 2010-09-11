package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.functions.IncLongFunction;
import org.multiverse.api.functions.LongFunction;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;

public class LongRefTranlocal_prepareForPoolingTest implements BetaStmConstants {

    private BetaObjectPool pool;
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    public void whenConstructed() {
        BetaLongRef ref = createLongRef(stm);

        LongRefTranlocal tranlocal = ref.___openForConstruction(pool);
        tranlocal.value = 200;

        tranlocal.prepareForPooling(pool);

        assertCleaned(tranlocal);
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
    public void whenIsCommuting() {
        BetaLongRef ref = createLongRef(stm);

        LongRefTranlocal tranlocal = ref.___openForCommute(pool);
        tranlocal.addCommutingFunction(IncLongFunction.INSTANCE_INC_ONE, pool);

        tranlocal.prepareForPooling(pool);

        assertCleaned(tranlocal);
    }

    @Test
    public void whenUpdate() {
        BetaLongRef ref = createLongRef(stm);
        LongRefTranlocal tranlocal = ref.___unsafeLoad().openForWrite(pool);
        tranlocal.value = 200;

        tranlocal.prepareForPooling(pool);

        assertCleaned(tranlocal);
    }

    @Test
    public void whenCommuting() {
        BetaLongRef ref = createLongRef(stm, 100);
        LongRefTranlocal tranlocal = ref.___openForCommute(pool);

        LongFunction function = mock(LongFunction.class);
        tranlocal.addCommutingFunction(function, pool);

        tranlocal.prepareForPooling(pool);

        assertCleaned(tranlocal);
    }

    private void assertCleaned(LongRefTranlocal tranlocal) {
        assertFalse(tranlocal.isPermanent);
        assertEquals(DIRTY_UNKNOWN, tranlocal.isDirty);
        assertEquals(0, tranlocal.value);
        assertFalse(tranlocal.isCommitted);
        assertFalse(tranlocal.isCommuting);
        assertNull(tranlocal.headCallable);
        assertNull(tranlocal.owner);
    }
}
