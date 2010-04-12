package org.multiverse.stms.alpha.programmatic;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.programmatic.ProgrammaticLong;
import org.multiverse.api.programmatic.ProgrammaticReferenceFactory;
import org.multiverse.stms.alpha.AlphaStm;

import static org.junit.Assert.assertEquals;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;

/**
 * @author Peter Veentjer
 */
public class AlphaProgrammaticReferenceFactory_atomicCreateLongTest {
    private AlphaStm stm;
    private ProgrammaticReferenceFactory refFactory;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
        refFactory = stm.getProgrammaticReferenceFactoryBuilder().build();
    }

    @Test
    public void test() {
        ProgrammaticLong ref = refFactory.atomicCreateLong(10);
        assertEquals(10, ref.atomicGet());
    }
}
