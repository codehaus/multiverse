package org.multiverse.benchmarks;

import org.benchy.Benchmark;
import org.benchy.TestCase;
import org.benchy.executor.BenchmarkExecutor;
import org.benchy.executor.DefaultBenchmarkExecutor;
import org.benchy.graph.GraphMain;
import org.benchy.repository.FileBasedBenchmarkResultRepository;

/**
 * @author Peter Veentjer
 */
public class BenchmarkMain {

    public static void main(String[] args) {
        FileBasedBenchmarkResultRepository repository = new FileBasedBenchmarkResultRepository();
        BenchmarkExecutor executor = new DefaultBenchmarkExecutor(repository);


        executor.execute(createReadPerformanceBenchmark());
        //executors.execute(createNonConcurrentUpdateBenchmark());
        // executors.execute(createConcurrentUpdateClassicLockBenchmark());
        // executors.execute(createConcurrentUpdateBenchmark());
        //executor.execute(createSetterInLoopDriver());

        GraphMain.main(new String[]{"/tmp",});
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
        benchmark.setDriverClass(PropertyAccessorDriver.class.getName());
        benchmark.setBenchmarkName("SetterInLoopDriver");
        benchmark.getTestCases().add(setterTestCase);
        benchmark.getTestCases().add(fieldTestCase);
        benchmark.getTestCases().add(localTestCase);
        return benchmark;
    }

    private static Benchmark createConcurrentUpdateBenchmark() {
        TestCase testCase = new TestCase();
        testCase.setProperty("incCountPerThread", 10 * 1000 * 1000);
        testCase.setProperty("threadCount", 1);

        Benchmark benchmark = new Benchmark();
        benchmark.setDriverClass(ConcurrentUpdateDriver.class.getName());
        benchmark.setBenchmarkName("ConcurrentUpdate");
        benchmark.getTestCases().add(testCase);
        return benchmark;
    }


    private static Benchmark createConcurrentUpdateClassicLockBenchmark() {
        TestCase testCase = new TestCase();
        testCase.setProperty("incCountPerThread", 10 * 1000 * 1000);
        testCase.setProperty("threadCount", 8);

        Benchmark benchmark = new Benchmark();
        benchmark.setDriverClass(ConcurrentUpdateWithIntrinsicLockDriver.class.getName());
        benchmark.setBenchmarkName("ConcurrentUpdateClassicLock");
        benchmark.getTestCases().add(testCase);
        return benchmark;
    }


    private static Benchmark createNonConcurrentUpdateBenchmark() {
        TestCase testCase = new TestCase();
        testCase.setProperty("incCountPerThread", 10 * 1000 * 1000);
        testCase.setProperty("threadCount", 8);

        Benchmark benchmark = new Benchmark();
        benchmark.setDriverClass(NonConcurrentUpdateDriver.class.getName());
        benchmark.setBenchmarkName("NonConcurrentUpdate");
        benchmark.getTestCases().add(testCase);
        return benchmark;
    }

    private static Benchmark createReadPerformanceBenchmark() {
        TestCase readonlyTestCase = new TestCase();
        readonlyTestCase.setProperty("readCountPerThread", 3 * 1000 * 1000);
        readonlyTestCase.setProperty("threadCount", 1);
        readonlyTestCase.setProperty("readonly", true);

        TestCase updateTestCase = new TestCase();
        updateTestCase.setProperty("readCountPerThread", 3 * 1000 * 1000);
        updateTestCase.setProperty("threadCount", 1);
        updateTestCase.setProperty("readonly", false);

        Benchmark benchmark = new Benchmark();
        benchmark.setDriverClass(ReadPerformanceDriver.class.getName());
        benchmark.setBenchmarkName("ReadPerformance");
        benchmark.getTestCases().add(readonlyTestCase);
        benchmark.getTestCases().add(updateTestCase);
        return benchmark;
    }
}
