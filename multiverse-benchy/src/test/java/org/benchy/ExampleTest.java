package org.benchy;

import org.benchy.executor.BenchmarkExecutor;
import org.benchy.executor.DefaultBenchmarkExecutor;
import org.benchy.repository.FileBasedBenchmarkResultRepository;
import org.junit.Before;
import org.junit.Test;


public class ExampleTest {
    private BenchmarkExecutor executor;

    @Before
    public void setUp() {
        executor = new DefaultBenchmarkExecutor(new FileBasedBenchmarkResultRepository());
    }

    @Test
    public void test() {
        TestCase testCase = new TestCase();

        Benchmark benchmark = new Benchmark();
        benchmark.setDriverClass(TestDriver.class);
        benchmark.setBenchmarkName("foo");
        benchmark.getTestCases().add(testCase);

        executor.execute(benchmark);
    }

    static class TestDriver extends AbstractBenchmarkDriver {

        @Override
        public void run() {
            System.out.println("run");
        }
    }
}
