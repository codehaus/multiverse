package org.multiverse.instrumentation.compiler;

/**
 * @author Peter Veentjer
 */
public interface Resolver {

    /**
     * @param classLoader
     * @param classname
     * @return
     */
    byte[] resolve(ClassLoader classLoader, String classname);
}
