package org.benchy;

import com.google.gdata.util.AuthenticationException;
import org.benchy.repository.GoogleSpreadsheetRepository;
import org.benchy.runner.BenchmarkRunner;
import org.benchy.runner.DefaultBenchmarkRunner;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;


@Ignore
public class ExampleTest {
    private BenchmarkRunner runner;

    @Before
    public void setUp() throws AuthenticationException {
        runner = new DefaultBenchmarkRunner(new GoogleSpreadsheetRepository("alarmnummer@gmail.com", "turdsandwitch"));
    }

    @Test
    public void test() {
        TestCase testCase = new TestCase();

        Benchmark benchmark = new Benchmark();
        benchmark.setDriverClass(TestDriver.class);
        benchmark.setBenchmarkName("foo");
        benchmark.getTestCases().add(testCase);

        runner.execute(benchmark);
    }

    static class TestDriver extends AbstractBenchmarkDriver {

        @Override
        public void run() {
            System.out.println("run");
        }
    }
}
