package org.multiverse.stms.alpha.compiler;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.compiler.MultiverseCompiler;

/**
 * @author Peter Veentjer
 */
public class CompilerTest {

    @Before
    public void setUp() {
    }

    @Test
    @Ignore
    public void test() {
        MultiverseCompiler.main(new String[]{
                "-compiler",
                "org.multiverse.stms.alpha.instrumentation.AlphaStmInstrumentor",
                "-targetDirectory",
                "/tmp/classes"
        });
    }
}
