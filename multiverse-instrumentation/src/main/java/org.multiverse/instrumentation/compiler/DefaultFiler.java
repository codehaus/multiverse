package org.multiverse.instrumentation.compiler;

/**
 * @author Peter Veentjer
 */
public class DefaultFiler implements Filer {

    @Override
    public void createClassFile(byte[] bytecode) {
        throw new RuntimeException();
    }
}
