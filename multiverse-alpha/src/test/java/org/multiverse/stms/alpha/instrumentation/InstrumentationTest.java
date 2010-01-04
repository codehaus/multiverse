package org.multiverse.stms.alpha.instrumentation;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.transactional.annotations.TransactionalObject;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaTransactionalObject;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;

/**
 * todo:
 * controleren dat de extra methodes er op zitten
 * controleren dat de velden van ongemodificeerde classes niet aangepast zijn
 * controleren dat de velden van gemodificeerde classes verwijderd zijn.
 */
public class InstrumentationTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
    }


    @Test
    public void nonTransactionalObjectIsNotTransformed() {
        NonTransactionalObject o = new NonTransactionalObject();
        assertFalse(o instanceof AlphaTransactionalObject);
    }

    static class NonTransactionalObject {
    }

    @Test
    public void statelessObjectIsNotTransformed() {
        StatelessObject object = new StatelessObject();
        assertFalse(object instanceof AlphaTransactionalObject);
    }

    @TransactionalObject
    public static class StatelessObject {
    }

    //=================== statics ===================

    @Test
    public void objectWithStaticFieldAndPrimitiveFieldIsTransformed() {
        ObjectWithStaticFieldAndPrimitiveField o = new ObjectWithStaticFieldAndPrimitiveField();
        assertTrue(o instanceof AlphaTransactionalObject);
    }

    @TransactionalObject
    public static class ObjectWithStaticFieldAndPrimitiveField {
        static int x;
        int y;
    }


    @Test
    public void objectWithOnlyStaticFieldIsNotTransformed() {
        ObjectWithStaticField o = new ObjectWithStaticField();
        assertFalse(o instanceof AlphaTransactionalObject);
    }

    @TransactionalObject
    public static class ObjectWithStaticField {
        static int x;
    }


    // ================= finals ================================

    @Test
    public void objectWithOnlyASingleFinalFieldIsNotTransformed() {
        ObjectWithOnlySingleFinalField o = new ObjectWithOnlySingleFinalField();
        assertFalse(o instanceof AlphaTransactionalObject);
        assertEquals(10, o.x);
    }


    @TransactionalObject
    public static class ObjectWithOnlySingleFinalField {
        final int x = 10;
    }

    @Test
    public void objectWithMultipleFinalFieldIsNotTransformed() {
        ObjectWithMultipleFinalField o = new ObjectWithMultipleFinalField();
        assertFalse(o instanceof AlphaTransactionalObject);
        assertEquals(10, o.x);
        assertEquals(20, o.y);
    }


    @TransactionalObject
    public static class ObjectWithMultipleFinalField {
        final int x = 10;
        final int y = 20;
    }

    @Test
    public void objectWithFinalsAndNonFinalsIsTransformed() {
        ObjectWithFinalsAndNonFinals o = new ObjectWithFinalsAndNonFinals();
        assertTrue(o instanceof AlphaTransactionalObject);

        assertEquals(10, o.x);
        assertEquals(20, o.y);
        assertEquals(30, o.getZ());
    }

    @TransactionalObject
    public static class ObjectWithFinalsAndNonFinals {
        final int x = 10;
        final int y = 20;
        int z = 30;

        public int getZ() {
            return z;
        }
    }

    // =============================================================


    //@Test

    public void objectWithOnlyMutablePrimitiveIsTransformed() {
        ObjectWithMutablePrimitive o = new ObjectWithMutablePrimitive();
        assertTrue(o instanceof AlphaTransactionalObject);
    }

    @TransactionalObject
    public static class ObjectWithMutablePrimitive {
        int value;
    }
}


