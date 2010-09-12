package org.multiverse.stms.beta.conflictcounters;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmUtils;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;

import static org.multiverse.TestUtils.assertNotEquals;

public class GlobalConflictCounterTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenCount() {
        GlobalConflictCounter globalConflictCounter = new GlobalConflictCounter(1);
        BetaLongRef ref = BetaStmUtils.newLongRef(stm);
        long oldCount = globalConflictCounter.count();
        globalConflictCounter.signalConflict(ref);

        assertNotEquals(oldCount, globalConflictCounter.count());
    }
}
