package org.multiverse.stms.alpha.compiler;

import org.junit.Before;
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
    public void test() {
        MultiverseCompiler.main(new String[]{
                "-compiler",
                "org.multiverse.stms.alpha.instrumentation.AlphaClazzCompiler",
                "-targetDirectory",
                "/tmp/classes"
        });
    }
}
