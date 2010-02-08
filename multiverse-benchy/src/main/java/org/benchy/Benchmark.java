package org.benchy;

import java.util.LinkedList;
import java.util.List;

/**
 * The Benchmark contains:
 * <ol>
 * <li>a List of testCase (the different variations)</li>
 * <li>the driver, contains the actual logic (no state)</li>
 * </ol>
 * A Benchmark should not be modified after it is fully constructed. So if nobody violates this, this object is
 * thread-safe to use.
 *
 * @author Peter Veentjer.
 */
public class Benchmark {

    private final List<TestCase> testCases = new LinkedList<TestCase>();
    private String benchmarkName;
    private String driverClass;

    public List<TestCase> getTestCases() {
        return testCases;
    }

    public String getBenchmarkName() {
        return benchmarkName;
    }

    public void setBenchmarkName(String benchmarkName) {
        this.benchmarkName = benchmarkName;
    }

    public String getDriverClass() {
        return driverClass;
    }

    public void setDriverClass(Class driverClass) {
        this.driverClass = driverClass.getName();
    }

    public void setDriverClass(String driverClass) {
        this.driverClass = driverClass;
    }

    public BenchmarkDriver loadDriver() {
        try {
            Class driverClass = Thread.currentThread().getContextClassLoader().loadClass(this.driverClass);
            return (BenchmarkDriver) driverClass.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize driver " + driverClass, e);
        }
    }
}
