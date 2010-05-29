package org.multiverse.stms.alpha.instrumentation.transactionalmethod;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalObject;
import org.multiverse.stms.alpha.AlphaStm;

import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * There was an instrumentation problem with an atomic method containing 'assert' statement. The symptom of the problem
 * is the following error:
 * <pre>
 * java.lang.ClassFormatError: Bad method name at constant pool index 326 in class file
 * org/multiverse/stms/alpha/instrumentation/fieldaccess/TransactionalMethod_assertTest$ObjectWithAsserts
 * at java.lang.ClassLoader.defineClass1(Native Method)
 * at java.lang.ClassLoader.defineClass(ClassLoader.java:621)
 * at java.security.SecureClassLoader.defineClass(SecureClassLoader.java:124)
 * </pre>
 * <p/>
 * This is a regression test to make sure that the issue isn't introduced again.
 *
 * @author Peter Veentjer.
 */
public class TransactionalMethod_assertTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    @Test
    public void test() {
        ObjectWithAsserts object = new ObjectWithAsserts("foo");
        object.setValue("bar");
    }

    @TransactionalObject
    public static class ObjectWithAsserts {

        private String value;

        public ObjectWithAsserts(String value) {
            //    assert value != null;
            this.value = value;
        }

        public void setValue(String newValue) {
            //    assert newValue != null;
            this.value = newValue;
        }
    }
}
