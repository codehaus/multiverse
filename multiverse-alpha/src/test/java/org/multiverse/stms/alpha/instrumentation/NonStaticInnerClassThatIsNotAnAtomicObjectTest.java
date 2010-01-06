package org.multiverse.stms.alpha.instrumentation;

import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.api.GlobalStmInstance.setGlobalStmInstance;
import org.multiverse.api.annotations.AtomicObject;
import org.multiverse.stms.alpha.AlphaStm;

/**
 * @author Peter Veentjer
 */
public class NonStaticInnerClassThatIsNotAnAtomicObjectTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = new AlphaStm();
        setGlobalStmInstance(stm);
    }

    @Test
    public void testAnonymousInnerClass() {
        AnonymousInnerClass o = new AnonymousInnerClass();
        assertNotNull(o.getRunnable());
    }

    @AtomicObject
    public static class AnonymousInnerClass {

        private Runnable runnable;

        public AnonymousInnerClass() {
            runnable = new Runnable() {
                @Override
                public void run() {
                }
            };
        }

        public Runnable getRunnable() {
            return runnable;
        }
    }

    @Test
    public void testNamedInnerClass() {
        NamedInnerClass o = new NamedInnerClass();
        assertNotNull(o.getNamedRunnable());
    }

    @AtomicObject
    public static class NamedInnerClass {

        private NamedRunnable someRunnable;

        public NamedInnerClass() {
            someRunnable = new NamedRunnable();
        }

        class NamedRunnable implements Runnable {
            @Override
            public void run() {
                //todo
            }
        }

        public NamedRunnable getNamedRunnable() {
            return someRunnable;
        }
    }
}