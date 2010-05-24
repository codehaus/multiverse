package org.benchy.runner;

import org.kohsuke.args4j.Option;

/**
 * @author Peter Veentjer
 */
public class RunnerArguments {

    @Option(name = "-r",
            usage = "directory containing the repository to store results of executed benchmarks " +
                    "(defaults to user.home/benchmarks)",
            required = false)
    public String repository = System.getProperty("user.home") + "/benchmarks";
}
