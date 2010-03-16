package org.multiverse.instrumentation.compiler;

import java.util.List;

/**
 * @author Peter Veentjer
 */
public class AbstractClazzPostProcessor implements ClazzPostProcessor {

    private final List<CompileStep> compileSteps;

    public AbstractClazzPostProcessor(List<CompileStep> compileSteps) {
        this.compileSteps = compileSteps;
    }

    @Override
    public Clazz process(Clazz clazz) {


        for (CompileStep step : compileSteps) {
            clazz = step.transform(null, clazz);
        }

        return clazz;
    }
}
