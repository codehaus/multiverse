package org.multiverse.stms.alpha.instrumentation;

import org.junit.Before;
import org.junit.Test;
import static org.multiverse.TestUtils.testIncomplete;
import static org.multiverse.api.GlobalStmInstance.setGlobalStmInstance;
import org.multiverse.stms.alpha.AlphaStm;

/**
 * @author Peter Veentjer
 */
public class StaticInnerClassThatIsNotAnAtomicObjectTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = new AlphaStm();
        setGlobalStmInstance(stm);
    }

    @Test
    public void test() {
        testIncomplete();
    }
}
