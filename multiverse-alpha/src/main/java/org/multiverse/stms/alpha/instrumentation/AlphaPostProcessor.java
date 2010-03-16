package org.multiverse.stms.alpha.instrumentation;

import org.multiverse.instrumentation.compiler.AbstractClazzPostProcessor;
import org.multiverse.instrumentation.compiler.CompileStep;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Peter Veentjer
 */
public class AlphaPostProcessor extends AbstractClazzPostProcessor {

    public AlphaPostProcessor() {
        super(createSteps());
    }

    private static List<CompileStep> createSteps() {
        List<CompileStep> steps = new LinkedList<CompileStep>();
        return steps;
    }
}
