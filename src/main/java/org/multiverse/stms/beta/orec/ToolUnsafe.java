package org.multiverse.stms.beta.orec;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * @author Peter Veentjer
 */
public class ToolUnsafe {
    private ToolUnsafe() {
    } // dummy private constructor

    /**
     * Fetch the Unsafe.  Use With Caution.
     *
     * @return an Unsafe instance.
     */
    public static Unsafe getUnsafe() {
        // Not on bootclasspath
        if (ToolUnsafe.class.getClassLoader() == null)
            return Unsafe.getUnsafe();
        try {
            final Field fld = Unsafe.class.getDeclaredField("theUnsafe");
            fld.setAccessible(true);
            return (Unsafe) fld.get(ToolUnsafe.class);
        } catch (Exception e) {
            throw new RuntimeException("Could not obtain access to sun.misc.Unsafe", e);
        }
    }
}