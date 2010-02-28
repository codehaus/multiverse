package org.multiverse.stms.alpha.instrumentation.asm;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.annotations.TransactionalObject;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaTransactionalObject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;

/**
 * @author Peter Veentjer
 */
public class TransactionalObject_SubclassTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
    }

    @Test
    @Ignore
    public void subclassingWithExplicitTxObjectAnnotation() {
        A a = new A();
        //assertTrue(a instanceof AlphaTransactionalObject);

        B.class.toString();

        B b = new B();

        assertTrue(b instanceof AlphaTransactionalObject);
    }

    @TransactionalObject
    public static class A {

        protected int x;
    }

    @TransactionalObject
    public static class B extends A {

        protected int y;
    }

    @Test
    public void testTxObjectExtendingOtherNonTxObject() {
        long version = stm.getVersion();

        Banana banana = new Banana();

        assertEquals(version + 1, stm.getVersion());
        assertEquals(0, banana.getLength());
    }

    @Test
    public void methodOnTxObject() {
        Banana banana = new Banana();

        long version = stm.getVersion();

        banana.setLength(100);

        assertEquals(version + 1, stm.getVersion());
        assertEquals(100, banana.getLength());
    }

    @Test
    public void methodOnSuper() {
        Banana banana = new Banana();

        long version = stm.getVersion();

        banana.setWeight(100);

        assertEquals(version, stm.getVersion());
        assertEquals(100, banana.getWeight());
    }

    @Test
    public void constructorThatCallsParametrizedSuper() {
        int weight = 10;
        int length = 20;

        long version = stm.getVersion();

        Banana banana = new Banana(weight, length);

        assertEquals(version + 1, stm.getVersion());
        assertEquals(weight, banana.getWeight());
        assertEquals(length, banana.getLength());
    }

    static class Fruit {

        private int weight;

        public Fruit() {
            weight = 1;
        }

        public Fruit(int weight) {
            this.weight = weight;
        }

        public void setWeight(int weight) {
            this.weight = weight;
        }

        public int getWeight() {
            return weight;
        }
    }

    @TransactionalObject
    static class Banana extends Fruit {

        private int length;

        Banana() {
            length = 0;
        }

        Banana(int length) {
            this.length = length;
        }

        Banana(int weight, int length) {
            super(weight);
            this.length = length;
        }

        public int getLength() {
            return length;
        }

        public void setLength(int length) {
            this.length = length;
        }
    }
}
