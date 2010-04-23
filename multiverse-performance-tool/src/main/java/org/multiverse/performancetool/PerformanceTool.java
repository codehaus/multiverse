package org.multiverse.performancetool;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Peter Veentjer
 */
public class PerformanceTool {

    public static void main(String[] args) throws InterruptedException {
        PerformanceToolArguments cli = createCli(args);
        PerformanceTool multiverseCompiler = new PerformanceTool();
        multiverseCompiler.run(cli);
    }

    private void run(PerformanceToolArguments arguments) throws InterruptedException {
        System.out.println("Multiverse: Starting Performance Tool");

        printSettings(arguments);
        printWarning();
        long durationNs = executeBenchmark(arguments);
        printResults(arguments, durationNs);
    }

    private void printResults(PerformanceToolArguments cli, long durationNs) {
        double transactionsPerSecondPerThread = (1.0d * cli.transactionCount * TimeUnit.SECONDS.toNanos(1)) / durationNs;
        double transactionsPerSecond = transactionsPerSecondPerThread * cli.threadCount;

        NumberFormat nf = createNumberFormat();

        System.out.println("[finished]----------------------------------------------------------");
        System.out.printf("Duration: %s seconds\n", TimeUnit.NANOSECONDS.toSeconds(durationNs));
        System.out.printf("Duration: %s nanoseconds\n", nf.format(durationNs));
        System.out.printf("Update transactions/second: %s \n", nf.format(transactionsPerSecond));
        System.out.printf("Update transactions/second: %s per core\n", nf.format(transactionsPerSecondPerThread));
    }

    private NumberFormat createNumberFormat() {
        return NumberFormat.getInstance(Locale.ENGLISH);
    }

    private long executeBenchmark(PerformanceToolArguments arguments) throws InterruptedException {
        AtomicLong clock = new AtomicLong();

        Barrier startBarrier = new Barrier();
        Thread[] threads = new Thread[arguments.threadCount];
        for (int k = 0; k < arguments.threadCount; k++) {
            threads[k] = new StressThread(k + 1, arguments.transactionCount, clock, arguments.strict, startBarrier);
            threads[k].start();
        }


        System.out.println("[starting]----------------------------------------------------------");

        long startNs = System.nanoTime();
        startBarrier.open();

        for (Thread thread : threads) {
            thread.join();
        }

        return System.nanoTime() - startNs;
    }

    private void printSettings(PerformanceToolArguments cli) {
        NumberFormat nf = createNumberFormat();
        System.out.println("Update transaction count per thread: " + nf.format(cli.transactionCount));
        System.out.println("Update transaction count in total: " + nf.format(cli.transactionCount * cli.threadCount));
        System.out.println("Threadcount:" + nf.format(cli.threadCount));
        System.out.println("Strict: " + cli.strict);
    }

    private void printWarning() {
        System.out.println("[info]---------------------------------------------------------");
        System.out.println("Watch out with hyperthreading since the number of virtual cores");
        System.out.println("increase but the actual number of cores remains the same. In");
        System.out.println("this benchmark hyperthreading could cause a performance");
        System.out.println("slowdown, because multiple threads will compete for the same");
        System.out.println("non virtual core. Use the -t argument for configuring the number");
        System.out.println("of threads.");
    }

    static class Barrier {
        private boolean open = false;

        public synchronized void open() {
            open = true;
            notifyAll();
        }

        public synchronized void awaitOpen() {
            while (!open) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private class StressThread extends Thread {
        private final long transactionCount;
        private final AtomicLong clock;
        private final boolean strict;
        private Barrier barrier;

        public StressThread(int id, long transactionCount, AtomicLong clock, boolean strict, Barrier barrier) {
            super("StressThread-" + id);
            this.transactionCount = transactionCount;
            this.clock = clock;
            this.strict = strict;
            this.barrier = barrier;
        }

        public void run() {
            NumberFormat nf = createNumberFormat();

            barrier.awaitOpen();

            for (long k = 0; k < transactionCount; k++) {
                if (strict) {
                    clock.incrementAndGet();
                } else {
                    long value = clock.get();
                    clock.compareAndSet(value, value + 1);
                }

                if (k > 0 && k % 10000000 == 0) {

                    System.out.printf("%s is at %s\n", getName(), nf.format(k));
                }
            }
        }
    }

    private static PerformanceToolArguments createCli(String[] args) {
        CmdLineParser parser = null;
        try {
            PerformanceToolArguments cli = new PerformanceToolArguments();
            parser = new CmdLineParser(cli);
            parser.parseArgument(args);
            return cli;
        } catch (CmdLineException e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
            System.err.println("java -jar myprogram.jar [options...]");
            if (parser != null) {
                parser.printUsage(System.out);
            }
            System.exit(-1);
            return null;
        }
    }

}
