package org.multiverse.stms.alpha.instrumentation;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.api.GlobalStmInstance.setGlobalStmInstance;
import org.multiverse.api.annotations.AtomicObject;
import org.multiverse.stms.alpha.AlphaAtomicObject;
import org.multiverse.stms.alpha.AlphaStm;

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
        stm = new AlphaStm();
        setGlobalStmInstance(stm);
    }


    @Test
    public void nonAtomicObjectIsNotTransformed() {
        NonAtomicObject o = new NonAtomicObject();
        assertFalse(o instanceof AlphaAtomicObject);
    }

    static class NonAtomicObject {
    }

    @Test
    public void statelessObjectIsNotTransformed() {
        StatelessObject object = new StatelessObject();
        assertFalse(object instanceof AlphaAtomicObject);
    }

    @AtomicObject
    public static class StatelessObject {
    }

    //=================== statics ===================

    //@Test
    public void objectWithStaticFieldAndPrimitiveFieldIsTransformed() {
        ObjectWithStaticFieldAndPrimitiveField o = new ObjectWithStaticFieldAndPrimitiveField();
        assertTrue(o instanceof AlphaAtomicObject);
    }

    @AtomicObject
    public static class ObjectWithStaticFieldAndPrimitiveField {
        static int x;
        int y;
    }


    @Test
    public void objectWithOnlyStaticFieldIsNotTransformed() {
        ObjectWithStaticField o = new ObjectWithStaticField();
        assertFalse(o instanceof AlphaAtomicObject);
    }

    @AtomicObject
    public static class ObjectWithStaticField {
        static int x;
    }


    // ================= finals ================================

    @Test
    public void objectWithOnlyASingleFinalFieldIsNotTransformed() {
        ObjectWithOnlySingleFinalField o = new ObjectWithOnlySingleFinalField();
        assertFalse(o instanceof AlphaAtomicObject);
        assertEquals(10, o.x);
    }


    @AtomicObject
    public static class ObjectWithOnlySingleFinalField {
        final int x = 10;
    }

    @Test
    public void objectWithMultipleFinalFieldIsNotTransformed() {
        ObjectWithMultipleFinalField o = new ObjectWithMultipleFinalField();
        assertFalse(o instanceof AlphaAtomicObject);
        assertEquals(10, o.x);
        assertEquals(20, o.y);
    }


    @AtomicObject
    public static class ObjectWithMultipleFinalField {
        final int x = 10;
        final int y = 20;
    }

    @Test
    public void objectWithFinalsAndNonFinalsIsTransformed() {
        ObjectWithFinalsAndNonFinals o = new ObjectWithFinalsAndNonFinals();
        assertTrue(o instanceof AlphaAtomicObject);

        assertEquals(10, o.x);
        assertEquals(20, o.y);
        assertEquals(30, o.getZ());
    }

    @AtomicObject
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
        assertTrue(o instanceof AlphaAtomicObject);
    }

    @AtomicObject
    public static class ObjectWithMutablePrimitive {
        int value;
    }
}


