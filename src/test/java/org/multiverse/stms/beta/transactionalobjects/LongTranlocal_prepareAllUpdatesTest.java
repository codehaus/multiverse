package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.assertNull;
import static org.multiverse.stms.beta.BetaStmTestUtils.assertVersionAndValue;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertHasNoCommitLock;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertHasNoUpdateLock;

public class LongTranlocal_prepareAllUpdatesTest implements BetaStmConstants {
    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    public void testWhenConflict() {
    }


    @Test
    public void readonly_whenNoReleaseObligation() {
        long initialValue = 10;
        BetaLongRef ref = new BetaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        LongRefTranlocal tranlocal = ref.___newTranlocal();
        tranlocal.setStatus(STATUS_READONLY);

        BetaTransaction tx = stm.startDefaultTransaction();
        tranlocal.prepareAllUpdates(pool, tx, 0);

        assertNull(ref.___getLockOwner());
        assertHasNoCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    @Ignore
    public void readonly_whenReleaseObligation() {

    }

    @Test
    @Ignore
    public void readonly_whenNoReleaseObligationAndEnsured() {

    }

    @Test
    @Ignore
    public void readonly_whenReleaseObligationAndEnsured() {

    }

    @Test
    @Ignore
    public void readonly_whenNoReleaseObligationAndPrivatized() {

    }

    @Test
    @Ignore
    public void readonly_whenReleaseObligationAndPrivatized() {

    }

    // ---------------------------------------------------------

    @Test
    @Ignore
    public void nonDirtyUpdate_whenNoReleaseObligation() {

    }

    @Test
    @Ignore
    public void nonDirtyUpdate_whenReleaseObligation() {

    }

    @Test
    @Ignore
    public void nonDirtyUpdate_whenNoReleaseObligationAndEnsured() {

    }

    @Test
    @Ignore
    public void nonDirtyUpdate_whenReleaseObligationAndEnsured() {

    }

    @Test
    @Ignore
    public void nonDirtyUpdate_whenNoReleaseObligationAndPrivatized() {

    }

    @Test
    @Ignore
    public void nonDirtyUpdate_whenReleaseObligationAndPrivatized() {

    }

    // ---------------------------------------------------------

    @Test
    @Ignore
    public void dirtyUpdate_whenNoReleaseObligation() {

    }

    @Test
    @Ignore
    public void dirtyUpdate_whenReleaseObligation() {

    }

    @Test
    @Ignore
    public void dirtyUpdate_whenNoReleaseObligationAndEnsured() {

    }

    @Test
    @Ignore
    public void dirtyUpdate_whenReleaseObligationAndEnsured() {

    }

    @Test
    @Ignore
    public void dirtyUpdate_whenNoReleaseObligationAndPrivatized() {

    }

    @Test
    @Ignore
    public void dirtyUpdate_whenReleaseObligationAndPrivatized() {

    }


    //todo: construction

    public void construction_whenNoChange() {

    }

    public void construction_whenChange() {

    }


}
