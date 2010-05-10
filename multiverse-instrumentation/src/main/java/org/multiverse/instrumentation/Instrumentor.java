package org.multiverse.instrumentation;

import java.io.File;

/**
 * The Instrumentor is responsible transforming a Clazz.
 * <p/>
 * The same Instrumentor can be used for compiletime instrumentation and loadtime instrumentation,
 * so no worries about that anymore.
 *
 * @author Peter Veentjer
 */
public interface Instrumentor {

    /**
     * Returns the name of this Instrumentor. Needed for identification and logging
     * purpuses.
     *
     * @return the name of this Instrumentor.
     * @see #getInstrumentorVersion()
     */
    String getInstrumentorName();

    /**
     * Returns the version of this Instrumentor. Needed for identification and
     * logging purposes.
     *
     * @return the version of this Instrumentor.
     * @see #getInstrumentorName()
     */
    String getInstrumentorVersion();

    /**
     * Returns the name of the Stm this Instrumentor is going to do the instrumentation
     * for. It is purely used for debugging/logging purposes.
     *
     * @return the name of the Stm
     */
    String getStmName();

    /**
     * Sets the Filer for this Instrumentor. The Filer can be used to do callbacks for
     * creating additional resources like classes.
     *
     * @param filer the filer this Instrumentor is going to use.
     */
    void setFiler(Filer filer);


    /**
     * If the bytecode generated by this Instrumentor should be dumped for debugging
     * purposes.
     *
     * @param dumpBytecode true if bytecode should be dumpted.
     * @see #setDumpDirectory(java.io.File)
     */
    void setDumpBytecode(boolean dumpBytecode);

    /**
     * Sets the location of this Instrumentor. The default is the tmp directory so
     * in most cases you don't need to worry about this property.
     *
     * @param dumpDirectory the directory to write the dumped classfiles to.
     */
    void setDumpDirectory(File dumpDirectory);

    File getDumpDirectory();

    /**
     * Sets the InstrumenterLogger this Instrumentor uses to execute log statements on.
     * <p/>
     * So if you want to have verbose output, just plug in some logger.
     *
     * @param logger
     */
    void setLog(InstrumenterLogger logger);

    /**
     * Add a pattern that is included. Default everything is included, unless it is explicitly
     * excluded. The pattern is just the
     *
     * @param included
     */
    void addIncluded(String included);

    void addExcluded(String ignored);

    /**
     * Processes a clazz. If nothing needs to be processed, the provided clazz can be returned. The return
     * value should never be null.
     *
     * @param originalClazz the Clazz to transform.
     * @return the transformed clazz. If extra classes need to be generated, they are created using the Filer.
     * @throws org.multiverse.instrumentation.CompileException
     *          if something goes wrong while compile clazz.
     */
    Clazz process(Clazz originalClazz);

    void setOptimize(boolean optimize);
}
