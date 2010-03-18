package org.multiverse.javaagent;

import org.multiverse.instrumentation.compiler.Clazz;
import org.multiverse.instrumentation.compiler.Filer;

import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.String.format;
import static org.multiverse.instrumentation.ClassUtils.defineClass;

/**
 * A {@link org.multiverse.instrumentation.compiler.Filer} implementation that feeds extra classes
 * that need to be generated to the JavaAgent.
 *
 * @author Peter Veentjer
 */
public class JavaAgentFiler implements Filer {

    private final static Logger logger = Logger.getLogger(JavaAgentFiler.class.getName());

    @Override
    public void createClassFile(Clazz clazz) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(format("Adding '%s' to the classloader", clazz.getName()));
        }

        defineClass(clazz.getClassLoader(),
                clazz.getName(),
                clazz.getBytecode());
    }
}
