package org.multiverse.stms.beta.conflictcounters;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.refs.LongRef;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.multiverse.TestUtils.assertNotEquals;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;

public class LocalConflictCounterTest {
    private BetaStm stm;

    @Before
    public void setUp(){
        stm = new BetaStm();
    }

    @Test
    public void test(){
        GlobalConflictCounter globalConflictCounter = new GlobalConflictCounter(1);

        LocalConflictCounter localConflictCounter = globalConflictCounter.createLocalConflictCounter();

        assertFalse(localConflictCounter.syncAndCheckConflict());

        LongRef ref = createLongRef(stm);
        globalConflictCounter.signalConflict(ref);
        long oldCount = localConflictCounter.get();

        assertTrue(localConflictCounter.syncAndCheckConflict());

        assertNotEquals(oldCount, localConflictCounter.get());
    }
}
