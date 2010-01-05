package org.benchy.executor;

import org.benchy.BenchmarkResult;
import org.benchy.BenchmarkResultRepository;
import org.benchy.InMemoryBenchmarkRepository;
import org.benchy.TestCaseResult;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * The BenchmarkRunner is responsible for executing a {@link Benchmark}.
 * <p/>
 * A BenchmarkRunner is not multithreaded itself (responsibility of the Driver) so can not be compared to the {@link
 * java.util.concurrent.Executor}.
 *
 * @author Peter Veentjer.
 */
public class BenchmarkExecutor {

    private BenchmarkResultRepository resultRepository;

    public BenchmarkExecutor() {
        this(new InMemoryBenchmarkRepository());
    }

    public BenchmarkExecutor(BenchmarkResultRepository resultRepository) {
        if (resultRepository == null) {
            throw new NullPointerException();
        }
        this.resultRepository = resultRepository;
    }

    public void execute(List<Benchmark> benchmarkList) {
        execute(benchmarkList.toArray(new Benchmark[]{}));
    }

    public void execute(Benchmark... benchmarks) {
        for (Benchmark benchmark : benchmarks) {
            doExecute(benchmark);
        }
    }

    private void doExecute(Benchmark benchmark) {
        List<TestCaseResult> resultList = new LinkedList<TestCaseResult>();

        printLine();
        System.out.println("Starting benchmark: " + benchmark.benchmarkName);
        printLine();

        long beginNs = System.nanoTime();

        for (TestCase testCase : benchmark.testCases) {
            executeTestCase(benchmark, resultList, testCase);
        }

        long endNs = System.nanoTime();
        long durationMs = TimeUnit.NANOSECONDS.toMillis(endNs - beginNs);

        BenchmarkResult benchmarkResult = new BenchmarkResult(benchmark.benchmarkName, resultList);
        resultRepository.store(benchmarkResult);
        printLine();
        System.out.printf("Finished benchmark: %s in %s ms, result of %s testcases stored\n",
                          benchmark.benchmarkName,
                          durationMs,
                          benchmarkResult.getTestCaseResultList().size());
        printLine();
    }

    private void executeTestCase(Benchmark benchmark, List<TestCaseResult> resultList, TestCase testCase) {
        warmup(benchmark, testCase);
        for (int attempt = 1; attempt <= testCase.getRunCount(); attempt++) {
            TestCaseResult testCaseResult = run(benchmark, testCase, attempt);
            resultList.add(testCaseResult);
        }
    }

    private void printLine() {
        System.out.println("----------------------------------------------------------------------");
    }

    private TestCaseResult run(Benchmark benchmark, TestCase testCase, int attempt) {
        BenchmarkDriver driver = benchmark.loadDriver();

        driver.preRun(testCase);
        TestCaseResult caseResult = new TestCaseResult(benchmark, testCase, attempt);
        System.out.printf("Starting executing attempt %s testcase: %s\n",
                          attempt,
                          benchmark.benchmarkName + " " + testCase.getPropertiesDescription());

        long startMs = System.currentTimeMillis();
        long startNs = System.nanoTime();
        driver.run();
        long endNs = System.nanoTime();
        long endMs = System.currentTimeMillis();
        long durationNs = endNs - startNs;

        caseResult.put("duration(ns)", durationNs);
        caseResult.put("start(ms)", startMs);
        caseResult.put("end(ms)", endMs);
        driver.postRun(caseResult);

        System.out.printf("Finished executing attempt %s of testcase: %s in %s ms\n",
                          attempt,
                          caseResult,
                          TimeUnit.NANOSECONDS.toMillis(durationNs));
        return caseResult;
    }

    private void warmup(Benchmark benchmark, TestCase testCase) {
        int warmupCount = testCase.getWarmupRunCount();

        System.out.printf("Starting %s warmup runs for testcase: %s\n",
                          warmupCount,
                          benchmark.benchmarkName + " " + testCase.getPropertiesDescription());

        for (int k = 0; k < testCase.getWarmupRunCount(); k++) {
            run(benchmark, testCase, k + 1);
        }

        System.out.printf("Finished %s warmup runs for testcase: %s\n",
                          warmupCount,
                          benchmark.benchmarkName + " " + testCase.getPropertiesDescription());
    }
}
