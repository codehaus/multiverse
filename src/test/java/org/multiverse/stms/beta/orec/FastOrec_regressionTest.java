package org.multiverse.stms.beta.orec;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.PanicError;
import org.multiverse.stms.beta.conflictcounters.GlobalConflictCounter;
import org.multiverse.stms.beta.transactionalobjects.LongRef;

import static org.junit.Assert.fail;
import static org.multiverse.stms.beta.orec.OrecTestUtils.makeReadBiased;

public class FastOrec_regressionTest {

    private GlobalConflictCounter globalConflictCounter;
    private LongRef dummyRef;

    @Before
    public void setUp() {
        globalConflictCounter = new GlobalConflictCounter(1);
        dummyRef = new LongRef();
    }

    @Test
    public void test1() {
        FastOrec orec = makeReadBiased(new FastOrec());

        //transaction 1
        orec.___arrive(1);
        //transaction 2
        orec.___arrive(1);

        //transaction 1
        orec.___tryUpdateLock(1);
        //transaction 1
        orec.___departAfterUpdateAndReleaseLock(globalConflictCounter, dummyRef);

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
        FastOrec orec = makeReadBiased(new FastOrec());

        //transaction 1
        orec.___arrive(1);
        //transaction 2
        orec.___arrive(1);

        //transaction 1 does the update
        orec.___tryUpdateLock(1);
        orec.___departAfterUpdateAndReleaseLock(globalConflictCounter, dummyRef);

        //transaction 2 now does the update.
        orec.___arriveAndLockForUpdate(1);
        orec.___departAfterUpdateAndReleaseLock(globalConflictCounter, dummyRef);

        System.out.println("orec: "+orec.___toOrecString());

        //transaction 2
        //try {
        //    orec.___tryUpdateLock(1);
        //    fail();
        //} catch (PanicError expected) {
        //}
    }
}