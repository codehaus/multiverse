package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.PanicError;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.conflictcounters.GlobalConflictCounter;

import static org.junit.Assert.fail;
import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;
import static org.multiverse.stms.beta.transactionalobjects.OrecTestUtils.makeReadBiased;

public class VeryAbstractBetaTransactionalObject_regressionTest {

    private GlobalConflictCounter globalConflictCounter;
    private BetaStm stm;

    @Before
    public void setUp() {
        globalConflictCounter = new GlobalConflictCounter(1);
        stm = new BetaStm();
    }

    @Test
    public void test1() {
        BetaTransactionalObject orec = makeReadBiased(newLongRef(stm));

        //transaction 1
        orec.___arrive(1);
        //transaction 2
        orec.___arrive(1);

        //transaction 1
        orec.___tryLockAfterNormalArrive(1, true);
        //transaction 1
        orec.___departAfterUpdateAndUnlock();

        //transaction 2   (this method should not be called since the the read was readbiased).  The ref does check for
        //only calling this method when it isn't read biased, but the exception still happens.
        try {
            orec.___departAfterFailure();
            fail();
        } catch (PanicError expected) {
        }
    }

    @Test
    public void test2() {
        BetaTransactionalObject  orec = makeReadBiased(newLongRef(stm));

        //transaction 1
        orec.___arrive(1);
        //transaction 2
        orec.___arrive(1);

        //transaction 1 does the update
        orec.___tryLockAfterNormalArrive(1, true);
        orec.___departAfterUpdateAndUnlock();

        //transaction 2 now does the update.
        orec.___tryLockAndArrive(1, true);
        orec.___departAfterUpdateAndUnlock();

        System.out.println("orec: " + orec.___toOrecString());

        //transaction 2
        //try {
        //    orec.___tryLockAfterArrive(1);
        //    fail();
        //} catch (PanicError expected) {
        //}
    }
}
