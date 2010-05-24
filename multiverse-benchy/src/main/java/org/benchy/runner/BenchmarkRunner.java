package org.benchy.runner;

import org.benchy.Benchmark;

/**
 * Responsible for executing a {@link Benchmark}.
 *
 * @author Peter Veentjer.
 */
public interface BenchmarkRunner {

    void execute(Benchmark... benchmarks);
}
