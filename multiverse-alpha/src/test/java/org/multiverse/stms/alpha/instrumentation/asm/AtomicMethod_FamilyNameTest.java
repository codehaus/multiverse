package org.multiverse.stms.alpha.instrumentation.asm;

import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.api.GlobalStmInstance.setGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;
import org.multiverse.api.annotations.AtomicMethod;
import org.multiverse.stms.alpha.AlphaStm;

public class AtomicMethod_FamilyNameTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = new AlphaStm();
        clearThreadLocalTransaction();
        setGlobalStmInstance(stm);
    }

    @Test
    public void providedFamilyName() {
        ProvidedFamilyName method = new ProvidedFamilyName();
        method.execute();

        assertEquals("provided", method.found);
    }

    private class ProvidedFamilyName {
        private String found;

        @AtomicMethod(familyName = "provided")
        public void execute() {
            found = getThreadLocalTransaction().getFamilyName();
        }
    }

    @Test
    public void defaultFamilyNameWithoutArguments() {
        DefaultFamilyName method = new DefaultFamilyName();
        method.execute();

        assertEquals("org.multiverse.stms.alpha.instrumentation.asm.AtomicMethod_FamilyNameTest$DefaultFamilyName.execute()", method.found);
    }

    @Test
    public void defaultFamilyWithArguments() {
        DefaultFamilyName method = new DefaultFamilyName();
        method.execute(1,true,"foo");

        assertEquals("org.multiverse.stms.alpha.instrumentation.asm.AtomicMethod_FamilyNameTest$DefaultFamilyName.execute(int,boolean,java.lang.String)", method.found);
    }


    private class DefaultFamilyName {
        private String found;

        @AtomicMethod
        public void execute() {
            found = getThreadLocalTransaction().getFamilyName();
        }

        @AtomicMethod
        public void execute(int a, boolean b, String c) {
            found = getThreadLocalTransaction().getFamilyName();
        }
    }
}
