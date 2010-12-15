import org.benchy.Benchmark
import org.benchy.GroovyTestCase
import org.multiverse.stms.beta.benchmarks.UncontendedAtomicIncrementAndGetDriver

def benchmark = new Benchmark()
benchmark.name = "uncontended_atomicAndIncrement"

for (def k in 1..8) {
    def testCase = new GroovyTestCase()
    testCase.name = "uncontended_atomicGetAndIncrement_with_${k}_threads"
    testCase.threadCount = k
    testCase.transactionsPerThread = 1000 * 1000 * 200
    testCase.driver = UncontendedAtomicIncrementAndGetDriver.class
    benchmark.add(testCase)
}
benchmark
