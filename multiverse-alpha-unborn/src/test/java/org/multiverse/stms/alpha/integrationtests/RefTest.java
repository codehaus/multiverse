package org.multiverse.stms.alpha.integrationtests;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaTransactionalObject;
import org.multiverse.transactional.refs.SimpleRef;

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
        SimpleRef<Integer> ref = new SimpleRef<Integer>();
        assertTrue(((Object) ref) instanceof AlphaTransactionalObject);
    }

    @Test
    public void refWithTypeParametersIsTransformed() {
        SimpleRef<Integer> ref = new SimpleRef<Integer>();
        assertTrue(((Object) ref) instanceof AlphaTransactionalObject);
    }
}
