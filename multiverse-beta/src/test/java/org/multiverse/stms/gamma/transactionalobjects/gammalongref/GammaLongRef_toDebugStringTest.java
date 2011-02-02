package org.multiverse.stms.gamma.transactionalobjects.gammalongref;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.GammaLongRef;

import static org.junit.Assert.assertEquals;

public class GammaLongRef_toDebugStringTest {

    private GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
    }

    @Test
    public void test() {
        GammaLongRef ref = new GammaLongRef(stm);
        String s = ref.toDebugString();
        assertEquals("GammaLongRef{orec=Orec(hasExclusiveLock=false, hasUpdateLock=false, readLocks=0, surplus=0, " +
                "isReadBiased=false, readonlyCount=0), version=1, value=0, hasListeners=false)", s);
    }
}
