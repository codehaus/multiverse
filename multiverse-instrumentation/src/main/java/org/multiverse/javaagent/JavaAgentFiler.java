package org.multiverse.javaagent;

import org.multiverse.instrumentation.compiler.Clazz;
import org.multiverse.instrumentation.compiler.Filer;

import static org.multiverse.instrumentation.ClassUtils.defineClass;

/**
 * A {@link org.multiverse.instrumentation.compiler.Filer} implementation that feeds extra classes
 * that need to be generated to the JavaAgent.
 *
 * @author Peter Veentjer
 */
public class JavaAgentFiler implements Filer {

    @Override
    public void createClassFile(Clazz clazz) {
        defineClass(clazz.getClassLoader(),
                clazz.getName(),
                clazz.getBytecode());
    }
}
