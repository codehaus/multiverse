package org.multiverse.stms.alpha.instrumentation.asm;

import static org.junit.Assert.assertFalse;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.utils.instrumentation.InstrumentationProblemMonitor;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

/**
 * This is a regression test that makes sure that the Multiverse Javaagent is able to deal with MBeanServer being used
 * as field/variable. There was an issue that caused a ClassCircularityError and this tests makes sure that the bug is
 * not introduced again.
 *
 * @author Peter Veentjer
 */
public class JmxMBeanServerRegressionTest {

    @Before
    public void setUp() {
        org.multiverse.TestUtils.resetInstrumentationProblemMonitor();
    }

    @Test
    @Ignore
    public void test() {
        Bean bean = new Bean();
        assertFalse(InstrumentationProblemMonitor.INSTANCE.isProblemFound());
    }

    public static class Bean {

        MBeanServer x = MBeanServerFactory.createMBeanServer();

        public Bean() {
            MBeanServer server = MBeanServerFactory.createMBeanServer();
        }

        public void instanceMethod() {
            MBeanServer server = MBeanServerFactory.createMBeanServer();
        }

        public static void staticMethod() {
            MBeanServer server = MBeanServerFactory.createMBeanServer();
        }
    }
}
