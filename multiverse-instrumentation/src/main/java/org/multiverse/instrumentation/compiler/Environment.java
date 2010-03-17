package org.multiverse.instrumentation.compiler;

import org.multiverse.instrumentation.metadata.MetadataRepository;

import java.io.File;

/**
 * @author Peter Veentjer
 */
public interface Environment {

    Filer getFiler();

    /**
     * True if transformed classes should be dumped.
     *
     * @return true if the class files should be dumped, false otherwise.
     */
    boolean dumpBytecode();

    /**
     * If verbose output should be generated.
     *
     * @return true if verbose output should be generated, false otherwise.
     */
    Log getLog();

    /**
     * Returns the directory where classfiles can be dumped.
     *
     * @return the directory where classfiles can be dumped.
     */
    File getDumpDirectory();

    MetadataRepository getMetadataRepository();
}
