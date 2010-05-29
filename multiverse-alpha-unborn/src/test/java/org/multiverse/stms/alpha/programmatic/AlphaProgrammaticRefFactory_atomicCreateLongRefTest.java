package org.multiverse.stms.alpha.programmatic;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.programmatic.ProgrammaticLongRef;
import org.multiverse.api.programmatic.ProgrammaticRefFactory;
import org.multiverse.stms.alpha.AlphaStm;

import static org.junit.Assert.assertEquals;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;

/**
 * @author Peter Veentjer
 */
public class AlphaProgrammaticRefFactory_atomicCreateLongRefTest {
    private AlphaStm stm;
    private ProgrammaticRefFactory refFactory;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
        refFactory = stm.getProgrammaticRefFactoryBuilder().build();
    }

    @Test
    public void test() {
        ProgrammaticLongRef ref = refFactory.atomicCreateLongRef(10);
        assertEquals(10, ref.atomicGet());
    }
}
