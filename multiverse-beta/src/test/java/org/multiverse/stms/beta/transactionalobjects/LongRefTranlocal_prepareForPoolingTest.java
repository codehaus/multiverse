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
import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;
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
    public void whenCheckConflict(){
        BetaLongRef ref = newLongRef(stm);

        BetaLongRefTranlocal tranlocal = ref.___newTranlocal();
        tranlocal.setIsConflictCheckNeeded(true);
        tranlocal.prepareForPooling(pool);

        assertPreparedForPooling(tranlocal);
    }

    @Test
    public void whenConstructed() {
        BetaLongRef ref = newLongRef(stm);

        BetaLongRefTranlocal tranlocal = ref.___newTranlocal();
        tranlocal.setStatus(STATUS_CONSTRUCTING);
        tranlocal.value = 200;

        tranlocal.prepareForPooling(pool);

        assertPreparedForPooling(tranlocal);
    }

    @Test
    public void whenEnsured() {
        BetaLongRef ref = newLongRef(stm, 100);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquireWriteLock(tx);

        BetaLongRefTranlocal tranlocal = (BetaLongRefTranlocal) tx.get(ref);
        tranlocal.prepareForPooling(pool);

        assertPreparedForPooling(tranlocal);
    }

    @Test
    public void whenPrivatized() {
        BetaLongRef ref = newLongRef(stm, 100);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquireCommitLock(tx);

        BetaLongRefTranlocal tranlocal = (BetaLongRefTranlocal) tx.get(ref);
        tranlocal.prepareForPooling(pool);

        assertPreparedForPooling(tranlocal);
    }

    @Test
    public void whenReadonlyAndPermanent() {
        BetaLongRef ref = makeReadBiased(newLongRef(stm, 100));

        BetaLongRefTranlocal tranlocal = ref.___newTranlocal();
        ref.___load(1, null, LOCKMODE_NONE, tranlocal);
        tranlocal.setStatus(STATUS_READONLY);

        tranlocal.prepareForPooling(pool);

        assertPreparedForPooling(tranlocal);
    }

    @Test
    public void whenNonPermanent() {
        BetaLongRef ref = newLongRef(stm, 100);

        BetaLongRefTranlocal tranlocal = ref.___newTranlocal();
        ref.___load(1, null, LOCKMODE_NONE, tranlocal);
        tranlocal.setStatus(STATUS_READONLY);

        tranlocal.prepareForPooling(pool);

        assertPreparedForPooling(tranlocal);
    }

    @Test
    public void whenUpdate() {
        BetaLongRef ref = newLongRef(stm);
        BetaLongRefTranlocal tranlocal = ref.___newTranlocal();
        ref.___load(1, null, LOCKMODE_NONE, tranlocal);

        tranlocal.value = 200;

        tranlocal.prepareForPooling(pool);

        assertPreparedForPooling(tranlocal);
    }

    @Test
    public void whenCommuting() {
        BetaLongRef ref = newLongRef(stm, 100);
        BetaLongRefTranlocal tranlocal = ref.___newTranlocal();
        tranlocal.setStatus(STATUS_COMMUTING);

        LongFunction function = mock(LongFunction.class);
        tranlocal.addCommutingFunction(function, pool);

        tranlocal.prepareForPooling(pool);

        assertPreparedForPooling(tranlocal);
    }

    private void assertPreparedForPooling(BetaLongRefTranlocal tranlocal) {
        assertFalse(tranlocal.hasDepartObligation());
        assertFalse(tranlocal.isDirty());
        assertEquals(0, tranlocal.value);
        assertEquals(0, tranlocal.oldValue);
        assertFalse(tranlocal.isReadonly());
        assertFalse(tranlocal.isCommuting());
        assertFalse(tranlocal.isConstructing());
        assertNull(tranlocal.headCallable);
        assertNull(tranlocal.owner);
        assertFalse(tranlocal.isConflictCheckNeeded());
        assertEquals(0, tranlocal.version);
    }
}
