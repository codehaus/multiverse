package org.multiverse.benchmarks;

import org.benchy.AbstractBenchmarkDriver;
import org.benchy.DriverParameter;
import org.benchy.TestCase;
import org.multiverse.annotations.TransactionalObject;

import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class ThisReadPerformanceDriver extends AbstractBenchmarkDriver {
    @DriverParameter
    private long readCount;
    private Ref ref;

    @Override
    public void preRun(TestCase testCase) {
        clearThreadLocalTransaction();
        ref = new Ref(readCount);
    }

    @Override
    public void run() {
        ref.run();
    }

    @TransactionalObject
    class Ref {
        private long value;

        private final long readCount;

        Ref(long readCount) {
            this.readCount = readCount;
        }

        public void run() {
            for (long k = 0; k < readCount; k++) {
                if (value == 10) {
                    throw new RuntimeException();
                }

                if (k % (10 * 1000 * 1000) == 0) {
                    System.out.println("at " + k);
                }
            }
        }
    }
}
