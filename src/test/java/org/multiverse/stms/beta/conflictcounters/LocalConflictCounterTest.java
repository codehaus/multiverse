package org.multiverse.stms.beta.conflictcounters;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.LongRef;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.assertNotEquals;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;

public class LocalConflictCounterTest {
    private BetaStm stm;

    @Before
    public void setUp(){
        stm = new BetaStm();
    }

    @Test
    public void whenConflictHappened(){
        GlobalConflictCounter global = new GlobalConflictCounter(1);

        LocalConflictCounter local = global.createLocalConflictCounter();

        LongRef ref = createLongRef(stm);
        global.signalConflict(ref);
        long oldCount = local.get();

        assertTrue(local.syncAndCheckConflict());

        assertNotEquals(oldCount, local.get());
    }

    @Test
    public void whenNoConflictHappened(){
        GlobalConflictCounter global = new GlobalConflictCounter(1);

        LocalConflictCounter local = global.createLocalConflictCounter();

        assertFalse(local.syncAndCheckConflict());
        assertSame(global.count(), local.get());
    }
}
