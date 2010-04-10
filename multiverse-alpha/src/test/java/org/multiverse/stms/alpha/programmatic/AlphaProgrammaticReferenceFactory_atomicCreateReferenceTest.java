package org.multiverse.stms.alpha.programmatic;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.programmatic.ProgrammaticReference;
import org.multiverse.api.programmatic.ProgrammaticReferenceFactory;
import org.multiverse.stms.alpha.AlphaStm;

import static org.junit.Assert.assertSame;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;

/**
 * @author Peter Veentjer
 */
public class AlphaProgrammaticReferenceFactory_atomicCreateReferenceTest {

    private AlphaStm stm;
    private ProgrammaticReferenceFactory refFactory;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
        refFactory = stm.getProgrammaticReferenceFactoryBuilder().build();
    }

    @Test
    public void test() {
        String value = "foo";
        ProgrammaticReference ref = refFactory.atomicCreateReference(value);
        assertSame(value, ref.atomicGet());
    }
}
