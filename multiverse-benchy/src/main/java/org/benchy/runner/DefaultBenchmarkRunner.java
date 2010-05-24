package org.benchy.runner;

import org.benchy.*;
import org.benchy.repository.BenchmarkResultRepository;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

/**
 * The BenchmarkRunner is responsible for executing a {@link Benchmark}.
 * <p/>
 * A BenchmarkRunner is not multi-threaded itself (responsibility of the Driver) so can not be compared to the
 * {@link java.util.concurrent.Executor}.
 *
 * @author Peter Veentjer.
 */
public class DefaultBenchmarkRunner implements BenchmarkRunner {

    private final BenchmarkResultRepository resultRepository;

    /**
     * Creates a new DefaultBenchmarkRunner with the provided BenchmarkResultRepository where it stores
     * the result.
     *
     * @param resultRepository the BenchmarkResultRepository that is used to write the {@link BenchmarkResult} to.
     * @throws NullPointerException if resultRepository is null.
     */
    public DefaultBenchmarkRunner(BenchmarkResultRepository resultRepository) {
        if (resultRepository == null) {
            throw new NullPointerException();
        }
        this.resultRepository = resultRepository;
    }

    @Override
    public void execute(Benchmark... benchmarks) {
        for (Benchmark benchmark : benchmarks) {
            doExecute(benchmark);
        }
    }

    private void doExecute(Benchmark benchmark) {
        if (benchmark == null) {
            throw new NullPointerException();
        }

        List<TestCaseResult> resultList = new LinkedList<TestCaseResult>();

        printLine();
        System.out.printf("Starting benchmark %s\n", benchmark.getBenchmarkName());
        printLine();

        long beginNs = System.nanoTime();

        for (TestCase testCase : benchmark.getTestCases()) {
            executeTestCase(benchmark, resultList, testCase);
        }

        long endNs = System.nanoTime();
        long durationMs = TimeUnit.NANOSECONDS.toMillis(endNs - beginNs);

        BenchmarkResult benchmarkResult = new BenchmarkResult(benchmark.getBenchmarkName(), resultList);
        resultRepository.store(benchmarkResult);
        printLine();
        System.out.printf("Finished benchmark: %s in %s ms, result of %s testcases stored\n",
                benchmark.getBenchmarkName(),
                durationMs,
                benchmarkResult.getTestCaseResults().size());
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
        BenchmarkDriver driver = setupDriver(benchmark, testCase);

        TestCaseResult caseResult = new TestCaseResult(benchmark, testCase, attempt);
        printLine();
        System.out.printf("Starting attempt %s testcase: %s\n",
                attempt,
                benchmark.getBenchmarkName());
        printProperties(testCase.getProperties());
        printLine();

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

        printLine();
        System.out.printf("Finished attempt %s  in %s ms\n",
                attempt,
                TimeUnit.NANOSECONDS.toMillis(durationNs));
        printProperties(caseResult.getProperties());
        printLine();
        return caseResult;
    }

    private void printProperties(Properties properties) {
        for (Object key : properties.keySet()) {
            System.out.printf("\t%s = %s\n", key, properties.getProperty((String) key));
        }
    }

    private BenchmarkDriver setupDriver(Benchmark benchmark, TestCase testCase) {
        BenchmarkDriver driver = benchmark.loadDriver();
        for (Field field : driver.getClass().getDeclaredFields()) {
            if (hasParameter(field)) {
                String value = testCase.getProperty(field.getName());
                if (value == null) {
                    String msg = format("field %s on driver %s has no value in the testcase",
                            field.getName(), driver.getClass().getName());
                    throw new RuntimeException(msg);
                }

                field.setAccessible(true);
                Class type = field.getType();
                try {
                    if (type.equals(Short.TYPE)) {
                        field.setShort(driver, Short.parseShort(value));
                    } else if (type.equals(Byte.TYPE)) {
                        field.setByte(driver, Byte.parseByte(value));
                    } else if (type.equals(Integer.TYPE)) {
                        field.setInt(driver, Integer.parseInt(value));
                    } else if (type.equals(Long.TYPE)) {
                        field.setLong(driver, Long.parseLong(value));
                    } else if (type.equals(Boolean.TYPE)) {
                        field.setBoolean(driver, Boolean.parseBoolean(value));
                    } else if (type.equals(Float.TYPE)) {
                        field.setFloat(driver, Float.parseFloat(value));
                    } else if (type.equals(Double.TYPE)) {
                        field.setDouble(driver, Double.parseDouble(value));
                    } else if (type.equals(String.class)) {
                        field.set(driver, value);
                    }
                } catch (Exception e) {
                    String msg = format("failed to set field %s on driver %s with value '%s'",
                            field.getName(), driver.getClass().getName(), value);
                    throw new RuntimeException(msg, e);
                }
            }
        }

        driver.preRun(testCase);
        return driver;
    }

    private boolean hasParameter(Field field) {
        return field.getAnnotation(DriverParameter.class) != null;
    }

    private void warmup(Benchmark benchmark, TestCase testCase) {
        int warmupCount = testCase.getWarmupRunCount();

        printLine();
        System.out.printf("Starting %s warmup runs for testcase: %s\n",
                warmupCount,
                benchmark.getBenchmarkName());
        printLine();

        for (int k = 0; k < testCase.getWarmupRunCount(); k++) {
            run(benchmark, testCase, k + 1);
        }

        printLine();
        System.out.printf("Finished %s warmup runs for testcase: %s\n",
                warmupCount,
                benchmark.getBenchmarkName());
        printProperties(testCase.getProperties());
        printLine();
    }
}
