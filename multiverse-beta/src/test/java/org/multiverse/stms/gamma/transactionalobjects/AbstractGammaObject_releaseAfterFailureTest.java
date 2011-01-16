package org.multiverse.stms.gamma.transactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.functions.LongFunction;
import org.multiverse.stms.gamma.GammaConstants;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactions.GammaTransaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.multiverse.stms.gamma.GammaTestUtils.assertRefHasNoLocks;
import static org.multiverse.stms.gamma.GammaTestUtils.assertVersionAndValue;

public class AbstractGammaObject_releaseAfterFailureTest implements GammaConstants {

    private GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
    }

    //todo: testen met en zonder depart obligation

    @Test
    public void whenCommuting() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        LongFunction function = mock(LongFunction.class);
        ref.commute(tx, function);
        GammaRefTranlocal tranlocal = tx.getRefTranlocal(ref);

        ref.releaseAfterFailure(tranlocal, tx.pool);

        assertRefHasNoLocks(ref);
        assertNull(tranlocal.owner);
        assertEquals(LOCKMODE_NONE, tranlocal.getLockMode());
        assertNull(tranlocal.headCallable);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    @Ignore
    public void whenRead() {

    }

    @Test
    @Ignore
    public void whenWrite() {

    }

    @Test
    @Ignore
    public void whenConstructing() {

    }
}
