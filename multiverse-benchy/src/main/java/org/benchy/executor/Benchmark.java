package org.benchy.executor;

import java.util.LinkedList;
import java.util.List;

/**
 * The Benchmark contains:
 * <ol>
 * <li>a List of testCase (the different variations)</li>
 * <li>the driver, contains the actual logic (no state)</li>
 * </ol>
 * A Benchmark should not be modified after it is fully constructed. So if nobody
 * violates this, this object is threadsafe to use.
 *
 * @author Peter Veentjer.
 */
public class Benchmark {

    public final List<TestCase> testCases = new LinkedList<TestCase>();
    public String benchmarkName;
    public String driverClass;

    public BenchmarkDriver loadDriver() {
        try {
            Class driverClass = Thread.currentThread().getContextClassLoader().loadClass(this.driverClass);
            return (BenchmarkDriver) driverClass.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize driver " + driverClass, e);
        }
    }
}
