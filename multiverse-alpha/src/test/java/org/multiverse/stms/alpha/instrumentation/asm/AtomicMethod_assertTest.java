package org.multiverse.stms.alpha.instrumentation.asm;

import org.junit.Before;
import org.junit.Test;
import static org.multiverse.api.GlobalStmInstance.setGlobalStmInstance;
import org.multiverse.api.annotations.AtomicObject;
import org.multiverse.stms.alpha.AlphaStm;

/**
 * There was an instrumentation problem with an atomic method containing 'assert' statement. The symptom of the problem
 * is the following error:
 * <pre>
 * java.lang.ClassFormatError: Bad method name at constant pool index 326 in class file
 * org/multiverse/stms/alpha/instrumentation/asm/AtomicMethod_assertTest$ObjectWithAsserts
 * at java.lang.ClassLoader.defineClass1(Native Method)
 * at java.lang.ClassLoader.defineClass(ClassLoader.java:621)
 * at java.security.SecureClassLoader.defineClass(SecureClassLoader.java:124)
 * </pre>
 *
 * @author Peter Veentjer.
 */
public class AtomicMethod_assertTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = new AlphaStm();
        setGlobalStmInstance(stm);
    }

    @Test
    public void test() {
        ObjectWithAsserts object = new ObjectWithAsserts("foo");
        object.setValue("bar");
    }

    @AtomicObject
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
