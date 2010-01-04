package org.multiverse.benchmarks;

import org.benchy.FileBasedBenchmarkResultRepository;
import org.benchy.executor.Benchmark;
import org.benchy.executor.BenchmarkExecutor;
import org.benchy.executor.TestCase;

/**
 * @author Peter Veentjer
 */
public class BenchmarkMain {

    public static void main(String[] args) {
        FileBasedBenchmarkResultRepository repository = new FileBasedBenchmarkResultRepository();
        BenchmarkExecutor executor = new BenchmarkExecutor(repository);


        //executors.execute(createReadPerformanceBenchmark());
        //executors.execute(createNonConcurrentUpdateBenchmark());
        // executors.execute(createConcurrentUpdateClassicLockBenchmark());
        // executors.execute(createConcurrentUpdateBenchmark());
        executor.execute(createSetterInLoopDriver());
    }

    private static Benchmark createSetterInLoopDriver() {
        TestCase fieldTestCase = new TestCase();
        fieldTestCase.setProperty("loopSize", 4 * (1000 * 1000 * 1000L));
        fieldTestCase.setProperty("writeType", "field");

        TestCase setterTestCase = new TestCase();
        setterTestCase.setProperty("loopSize", 4 * (1000 * 1000 * 1000L));
        setterTestCase.setProperty("writeType", "setter");

        TestCase localTestCase = new TestCase();
        localTestCase.setProperty("loopSize", 4 * (1000 * 1000 * 1000L));
        localTestCase.setProperty("writeType", "local");

        Benchmark benchmark = new Benchmark();
        benchmark.driverClass = PropertyAccessorDriver.class.getName();
        benchmark.benchmarkName = "SetterInLoopDriver";
        benchmark.testCases.add(setterTestCase);
        benchmark.testCases.add(fieldTestCase);
        benchmark.testCases.add(localTestCase);
        return benchmark;
    }

    private static Benchmark createConcurrentUpdateBenchmark() {
        TestCase testCase = new TestCase();
        testCase.setProperty("incCountPerThread", 10 * 1000 * 1000);
        testCase.setProperty("threadCount", 1);

        Benchmark benchmark = new Benchmark();
        benchmark.driverClass = ConcurrentUpdateDriver.class.getName();
        benchmark.benchmarkName = "ConcurrentUpdate";
        benchmark.testCases.add(testCase);
        return benchmark;
    }


    private static Benchmark createConcurrentUpdateClassicLockBenchmark() {
        TestCase testCase = new TestCase();
        testCase.setProperty("incCountPerThread", 10 * 1000 * 1000);
        testCase.setProperty("threadCount", 8);

        Benchmark benchmark = new Benchmark();
        benchmark.driverClass = ConcurrentUpdateWithIntrinsicLockDriver.class.getName();
        benchmark.benchmarkName = "ConcurrentUpdateClassicLock";
        benchmark.testCases.add(testCase);
        return benchmark;
    }


    private static Benchmark createNonConcurrentUpdateBenchmark() {
        TestCase testCase = new TestCase();
        testCase.setProperty("incCountPerThread", 10 * 1000 * 1000);
        testCase.setProperty("threadCount", 8);

        Benchmark benchmark = new Benchmark();
        benchmark.driverClass = NonConcurrentUpdateDriver.class.getName();
        benchmark.benchmarkName = "NonConcurrentUpdate";
        benchmark.testCases.add(testCase);
        return benchmark;
    }

    private static Benchmark createReadPerformanceBenchmark() {
        TestCase readonlyTestCase = new TestCase();
        readonlyTestCase.setProperty("readCountPerThread", 10 * 1000 * 1000);
        readonlyTestCase.setProperty("threadCount", 10);
        readonlyTestCase.setProperty("readonly", true);

        TestCase updateTestCase = new TestCase();
        updateTestCase.setProperty("readCountPerThread", 10 * 1000 * 1000);
        updateTestCase.setProperty("threadCount", 10);
        updateTestCase.setProperty("readonly", false);

        Benchmark benchmark = new Benchmark();
        benchmark.driverClass = ReadPerformanceDriver.class.getName();
        benchmark.benchmarkName = "ReadPerformance";
        benchmark.testCases.add(readonlyTestCase);
        benchmark.testCases.add(updateTestCase);
        return benchmark;
    }
}
