package org.multiverse.stms.alpha.instrumentation.asm;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.transactional.annotations.TransactionalObject;
import org.multiverse.stms.alpha.AlphaStm;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;

public class DebugInfoIsNotLostTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
    }

    @Test
    public void test() {
        Foo foo = new Foo();
        try {
            foo.throwException();
            fail();
        } catch (RuntimeException ex) {
            System.out.println("---------------- expected stacktrace ---------------------");
            ex.printStackTrace(System.out);
            System.out.println("---------------- expected stacktrace ---------------------");
            StackTraceElement[] element = ex.getStackTrace();
            assertTrue(element[0].getLineNumber() >= 0);
            assertEquals("DebugInfoIsNotLostTest.java", element[0].getFileName());
            assertTrue(element[1].getLineNumber() >= 0);
            assertEquals("DebugInfoIsNotLostTest.java", element[1].getFileName());
        }
    }

    @TransactionalObject
    public class Foo {

        private int someField;

        public Foo() {
            this.someField = 0;
        }

        public int throwException() {
            throw new RuntimeException();
        }
    }
}
