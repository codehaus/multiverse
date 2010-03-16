package org.multiverse.instrumentation.compiler;

/**
 * @author Peter Veentjer
 */
public class AbstractCompileStep implements CompileStep {

    private String name;

    public AbstractCompileStep() {

    }

    @Override
    public Clazz transform(Environment environment, Clazz clazz) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
