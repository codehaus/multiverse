package org.multiverse.performancetool;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * Simple class to obtain access to the {@link Unsafe} object.  
 */
class UtilUnsafe {
    private UtilUnsafe() {
    } // dummy private constructor

    /**
     * Fetch the Unsafe.  Use With Caution.
     */
    public static Unsafe getUnsafe() {
        // Not on bootclasspath
        if (UtilUnsafe.class.getClassLoader() == null)
            return Unsafe.getUnsafe();
        try {
            final Field fld = Unsafe.class.getDeclaredField("theUnsafe");
            fld.setAccessible(true);
            return (Unsafe) fld.get(UtilUnsafe.class);
        } catch (Exception e) {
            throw new RuntimeException("Could not obtain access to sun.misc.Unsafe", e);
        }
    }
}