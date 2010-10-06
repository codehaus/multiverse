package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.functions.LongFunction;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.multiverse.stms.beta.BetaStmUtils.newLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.makeReadBiased;

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
        BetaLongRef ref = newLongRef(stm);

        LongRefTranlocal tranlocal = ref.___newTranlocal();
        tranlocal.isConstructing = true;
        tranlocal.value = 200;

        tranlocal.prepareForPooling(pool);

        assertPreparedForPooling(tranlocal);
    }

    @Test
    public void whenEnsured() {
        BetaLongRef ref = newLongRef(stm, 100);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.ensure(tx);

        LongRefTranlocal tranlocal = (LongRefTranlocal) tx.get(ref);
        tranlocal.prepareForPooling(pool);

        assertPreparedForPooling(tranlocal);
    }

    @Test
    public void whenPrivatized() {
        BetaLongRef ref = newLongRef(stm, 100);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.privatize(tx);

        LongRefTranlocal tranlocal = (LongRefTranlocal)tx.get(ref);
        tranlocal.prepareForPooling(pool);

        assertPreparedForPooling(tranlocal);
    }

    @Test
    public void whenCommittedAndPermanent() {
        BetaLongRef ref = makeReadBiased(newLongRef(stm, 100));

        LongRefTranlocal tranlocal = ref.___newTranlocal();
        ref.___load(1, null, LOCKMODE_NONE, tranlocal);
        tranlocal.isCommitted = true;

        tranlocal.prepareForPooling(pool);

        assertPreparedForPooling(tranlocal);
    }

    @Test
    public void whenNonPermanent() {
        BetaLongRef ref = newLongRef(stm, 100);

        LongRefTranlocal tranlocal = ref.___newTranlocal();
        ref.___load(1, null, LOCKMODE_NONE, tranlocal);
        tranlocal.isCommitted = true;

        tranlocal.prepareForPooling(pool);

        assertPreparedForPooling(tranlocal);
    }

    @Test
    public void whenUpdate() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal tranlocal = ref.___newTranlocal();
        ref.___load(1, null, LOCKMODE_NONE, tranlocal);

        tranlocal.value = 200;

        tranlocal.prepareForPooling(pool);

        assertPreparedForPooling(tranlocal);
    }

    @Test
    public void whenCommuting() {
        BetaLongRef ref = newLongRef(stm, 100);
        LongRefTranlocal tranlocal = ref.___newTranlocal();
        tranlocal.isCommuting = true;

        LongFunction function = mock(LongFunction.class);
        tranlocal.addCommutingFunction(function, pool);

        tranlocal.prepareForPooling(pool);

        assertPreparedForPooling(tranlocal);
    }

    private void assertPreparedForPooling(LongRefTranlocal tranlocal) {
        assertFalse(tranlocal.hasDepartObligation);
        assertFalse(tranlocal.isDirty);
        assertEquals(0, tranlocal.value);
        assertEquals(0, tranlocal.oldValue);
        assertFalse(tranlocal.isCommitted);
        assertFalse(tranlocal.isCommuting);
        assertFalse(tranlocal.isConstructing);
        assertNull(tranlocal.headCallable);
        assertNull(tranlocal.owner);
        assertEquals(0, tranlocal.version);
    }
}
