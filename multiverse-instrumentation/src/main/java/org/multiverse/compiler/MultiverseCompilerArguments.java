package org.multiverse.compiler;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

/**
 * @author Peter Veentjer
 */
public class MultiverseCompilerArguments {

    @Option(name = "-d", usage = "dump the bytecode for debugging purposes", required = false)
    public boolean dumpBytecode;

    @Option(name = "-v", usage = "verbose output", required = false)
    public boolean verbose;

    @Argument(required = true, index = 0, metaVar = "COMPILER",
            usage = "the org.multiverse.instrumentation.compiler.ClazzCompiler to use")
    public String compilerName;

    @Argument(required = true, index = 1, metaVar = "TARGET_DIRECTORY",
            usage = "target directory with the classes to transform")
    public String targetDirectory;
}
