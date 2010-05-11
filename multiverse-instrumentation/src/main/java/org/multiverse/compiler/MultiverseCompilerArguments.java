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

    @Option(name = "-o", usage = "optimize the classes (should not be combined with agent)", required = false)
    public boolean optimize = true;

    @Option(name = "-i", usage = "the org.multiverse.instrumentation.Instrumentor to use. Defaults to" +
            "org.multiverse.stms.alpha.instrumentation.AlphaStmInstrumentor", required = false)
    public String instrumentorName = "org.multiverse.stms.alpha.instrumentation.AlphaStmInstrumentor";

    @Argument(required = true, index = 0, metaVar = "TARGET_DIRECTORY",
            usage = "target directory with the classes to transform")
    public String targetDirectory;
}

