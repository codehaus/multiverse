package org.benchy;

import java.util.Date;

public class InMemoryBenchmarkRepository implements BenchmarkResultRepository{

    @Override
    public BenchmarkResult load(Date date, String benchmarkName) {
        throw new RuntimeException();
    }

    @Override
    public BenchmarkResult loadLast(Date date, String benchmarkName) {
        throw new RuntimeException();
    }

    @Override
    public BenchmarkResult loadLast(String benchmark) {
        throw new RuntimeException();
    }

    @Override
    public void store(BenchmarkResult benchmarkResult) {
        throw new RuntimeException();
    }
}
