package org.multiverse.instrumentation.compiler;

import java.io.File;

/**
 * The ClazzCompiler is responsible transforming a Clazz.
 * <p/>
 * The same clazzCompiler can be used for compiletime instrumentation and loadtime instrumentation,
 * so no worries about that anymore.
 *
 * @author Peter Veentjer
 */
public interface ClazzCompiler {

    void setFiler(Filer filer);

    void setResolver(Resolver resolver);

    void setDumpBytecode(boolean dumpBytecode);

    void setDumpDirectory(File dumpDirectory);

    void addExcluded(String ignored);

    /**
     * Sets the Log this ClazzCompiler uses to execute log statements on.
     * <p/>
     * So if you want to have verbose output, just plug in some logger.
     *
     * @param logger
     */
    void setLog(Log logger);

    /**
     * Add a pattern that is included. Default everything is included, unless it is explicitly
     * excluded. The pattern is just the
     *
     * @param included
     */
    void addIncluded(String included);

    /**
     * Processes a clazz. If nothing needs to be processed, the provided clazz can be returned. The return
     * value should never be null.
     *
     * @param originalClazz the Clazz to transform.
     * @return the transformed clazz. If extra classes need to be generated, they are created using the Filer.
     * @throws CompileException if something goes wrong while compile clazz.
     */
    Clazz process(Clazz originalClazz);
}
