package org.multiverse.instrumentation.compiler;

import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.String.format;

/**
 * @author Peter Veentjer
 */
public abstract class AbstractCompilePhase implements CompilePhase {

    private final static Logger logger = Logger.getLogger(AbstractCompilePhase.class.getName());

    private final String name;

    public AbstractCompilePhase(String name) {
        if (name == null) {
            throw new NullPointerException();
        }

        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public Clazz compile(Environment environment, Clazz clazz) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(format("Compilephase %s is transforming class %s", name, clazz.getName()));
        }

        doInit();

        return doCompile(environment, clazz);
    }

    @Override
    public String toString() {
        return name;
    }

    protected void doInit() {
    }

    protected abstract Clazz doCompile(Environment environment, Clazz clazz);
}
