package org.multiverse.stms.alpha.integrationtests;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaTransactionalObject;
import org.multiverse.transactional.DefaultTransactionalReference;

import static org.junit.Assert.assertTrue;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;

public class RefTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
    }

    @Test
    public void refIsTransformed() {
        DefaultTransactionalReference<Integer> ref = new DefaultTransactionalReference<Integer>();
        assertTrue(((Object) ref) instanceof AlphaTransactionalObject);
    }

    @Test
    public void refWithTypeParametersIsTransformed() {
        DefaultTransactionalReference<Integer> ref = new DefaultTransactionalReference<Integer>();
        assertTrue(((Object) ref) instanceof AlphaTransactionalObject);
    }
}
