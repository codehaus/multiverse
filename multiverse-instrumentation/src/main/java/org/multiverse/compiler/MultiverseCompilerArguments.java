package org.multiverse.compiler;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Veentjer
 */
public class MultiverseCompilerArguments {

    @Option(name = "d", usage = "dump the bytecode for debugging purposes")
    public File out;

    // receives other command line parameters than options
    @Argument
    public List arguments = new ArrayList();

    @Option(name = "-d", usage = "output to this file", required = false)
    public boolean dumpBytecode;


    @Argument(required = true, index = 1, metaVar = "COMPILER",
            usage = "the compiler to use")
    public String compilerName;


    @Argument(required = true, index = 2, metaVar = "TARGET_DIRECTORY",
            usage = "target directory")
    public File targetDirectory;

}
