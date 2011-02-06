package org.multiverse.stms.gamma.transactionalobjects.gammabooleanref;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.GammaBooleanRef;

import static org.junit.Assert.assertEquals;

public class GammaBooleanRef_toDebugStringTest {

    private GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
    }

    @Test
    public void test() {
        GammaBooleanRef ref = new GammaBooleanRef(stm);
        String s = ref.toDebugString();
        assertEquals("GammaBooleanRef{orec=Orec(hasExclusiveLock=false, hasWriteLock=false, readLocks=0, surplus=0, " +
                "isReadBiased=false, readonlyCount=0), version=1, value=false, hasListeners=false)", s);
    }
}
