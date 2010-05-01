package org.multiverse.utils;

/**
 * Prevents having unwanted System.outs all over the place. Sonar will pick this one one and of course all the
 * unwanted ones.
 *
 * @author Peter Veentjer
 */
public final class SystemOut {

    public static void println(String s, Object... args) {
        System.out.printf(s, args);
        System.out.println();
    }

    //we don't want instances.

    private SystemOut() {
    }
}
