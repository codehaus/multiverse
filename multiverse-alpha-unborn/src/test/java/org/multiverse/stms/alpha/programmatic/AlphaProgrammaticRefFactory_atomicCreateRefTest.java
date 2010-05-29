package org.multiverse.stms.alpha.programmatic;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.programmatic.ProgrammaticRef;
import org.multiverse.api.programmatic.ProgrammaticRefFactory;
import org.multiverse.stms.alpha.AlphaStm;

import static org.junit.Assert.assertSame;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;

/**
 * @author Peter Veentjer
 */
public class AlphaProgrammaticRefFactory_atomicCreateRefTest {

    private AlphaStm stm;
    private ProgrammaticRefFactory refFactory;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
        refFactory = stm.getProgrammaticRefFactoryBuilder().build();
    }

    @Test
    public void test() {
        String value = "foo";
        ProgrammaticRef ref = refFactory.atomicCreateRef(value);
        assertSame(value, ref.atomicGet());
    }
}
