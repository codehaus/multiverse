package org.benchy.executor;

import org.benchy.Benchmark;

/**
 * Responsible for executing a {@link Benchmark}.
 *
 * @author Peter Veentjer.
 */
public interface BenchmarkExecutor {

    void execute(Benchmark... benchmarks);
}
