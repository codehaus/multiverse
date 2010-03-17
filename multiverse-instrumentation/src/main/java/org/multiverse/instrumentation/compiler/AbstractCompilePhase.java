package org.multiverse.instrumentation.compiler;

/**
 * @author Peter Veentjer
 */
public abstract class AbstractCompilePhase implements CompilePhase {

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
