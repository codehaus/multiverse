package org.multiverse.stms.alpha.instrumentation.asm;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.stms.alpha.AlphaStm;

import static org.junit.Assert.assertEquals;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

public class TransactionalMethod_FamilyNameTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    @Test
    public void providedFamilyName() {
        ProvidedFamilyName method = new ProvidedFamilyName();
        method.execute();

        assertEquals("provided", method.found);
    }

    private class ProvidedFamilyName {
        private String found;

        @TransactionalMethod(familyName = "provided")
        public void execute() {
            found = getThreadLocalTransaction().getConfig().getFamilyName();
        }
    }

    @Test
    public void defaultFamilyNameWithoutArguments() {
        DefaultFamilyName method = new DefaultFamilyName();
        method.execute();

        assertEquals("org.multiverse.stms.alpha.instrumentation.asm.TransactionalMethod_FamilyNameTest$DefaultFamilyName.execute()", method.found);
    }

    @Test
    public void defaultFamilyWithArguments() {
        DefaultFamilyName method = new DefaultFamilyName();
        method.execute(1, true, "foo");

        assertEquals("org.multiverse.stms.alpha.instrumentation.asm.TransactionalMethod_FamilyNameTest$DefaultFamilyName.execute(int,boolean,java.lang.String)", method.found);
    }


    private class DefaultFamilyName {
        private String found;

        @TransactionalMethod
        public void execute() {
            found = getThreadLocalTransaction().getConfig().getFamilyName();
        }

        @TransactionalMethod
        public void execute(int a, boolean b, String c) {
            found = getThreadLocalTransaction().getConfig().getFamilyName();
        }
    }
}
