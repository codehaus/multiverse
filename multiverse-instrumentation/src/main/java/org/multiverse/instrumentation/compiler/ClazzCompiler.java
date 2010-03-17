package org.multiverse.instrumentation.compiler;

import org.multiverse.instrumentation.Filer;
import org.multiverse.instrumentation.Resolver;

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

    void prepare();

    /**
     * Processes a clazz. If nothing needs to be processed, the provided clazz can be returned. The return
     * value should never be null.
     *
     * @param clazz the Clazz to transform.
     * @return the transformed clazz. It is an array because additional new classes can be generated.
     * @throws CompileException
     */
    Clazz process(Clazz clazz);

    void setDumpBytecode(boolean b);

    void setDumpDirectory(File file);
}
