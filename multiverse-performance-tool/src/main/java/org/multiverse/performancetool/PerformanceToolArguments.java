package org.multiverse.performancetool;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

/**
 * @author Peter Veentjer
 */
public class PerformanceToolArguments {

    @Option(name = "-t",
            required = false,
            usage = "The number of threads (default is number of available processors")
    public int threadCount = Runtime.getRuntime().availableProcessors();

    @Argument(required = false, index = 0, metaVar = "TRANSACTION_COUNT",
            usage = "The number of transactions per thread")
    public long transactionCount = 200 * 1000 * 1000;

    @Option(name = "-s", usage = "If the tick should be strict (multiverse can deal with relaxed ticks)", required = false)
    public boolean strict = false;


}
