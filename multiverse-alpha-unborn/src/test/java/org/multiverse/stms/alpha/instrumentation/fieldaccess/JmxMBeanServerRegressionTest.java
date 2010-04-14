package org.multiverse.stms.alpha.instrumentation.fieldaccess;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalObject;
import org.multiverse.instrumentation.InstrumentationTestUtils;
import org.multiverse.javaagent.JavaAgentProblemMonitor;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

import static org.junit.Assert.assertFalse;

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
        InstrumentationTestUtils.resetInstrumentationProblemMonitor();
    }

    @Test
    public void test() {
        ProblemCausingObject object = new ProblemCausingObject();
        assertFalse(JavaAgentProblemMonitor.INSTANCE.isProblemFound());
    }

    @TransactionalObject
    public static class ProblemCausingObject {

        MBeanServer x = MBeanServerFactory.createMBeanServer();

        public ProblemCausingObject() {
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
