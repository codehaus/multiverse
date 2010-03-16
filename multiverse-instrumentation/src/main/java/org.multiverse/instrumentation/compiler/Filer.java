package org.multiverse.instrumentation.compiler;

/**
 * @author Peter Veentjer
 */
public interface Filer {

    void createClassFile(byte[] bytecode);
}
