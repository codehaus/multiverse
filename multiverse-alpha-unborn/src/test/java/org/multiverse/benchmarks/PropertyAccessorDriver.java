package org.multiverse.benchmarks;

import org.benchy.AbstractBenchmarkDriver;
import org.benchy.DriverParameter;
import org.benchy.TestCase;
import org.benchy.TestCaseResult;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;

/**
 * A Driver that tests the overhead of field access in transactions. Within a loop
 * a field is set using 3 different writeTypes:
 * <ol>
 * <li>setter; each update is directly written using a setter</li>
 * <li>direct; each update is directly written using put_field</li>
 * <li>local; each update is stored in a local variable, and once the loop completes
 * the value is written. These would give the least overhead</li>
 * </ol>
 * It appears that the performance differences between the mechanisms are not that big.
 *
 * @author Peter Veentjer
 */
public class PropertyAccessorDriver extends AbstractBenchmarkDriver {
    @DriverParameter
    private long loopSize;

    @DriverParameter
    private String writeType;

    private LongRef ref;

    public void preRun(TestCase testCase) {
        ref = new LongRef();
    }

    @Override
    public void run() {
        if (writeType.equals("field")) {
            ref.loopField(loopSize);
        } else if (writeType.equals("setter")) {
            ref.loopSetter(loopSize);
        } else if (writeType.equals("local")) {
            ref.loopLocal(loopSize);
        } else {
            throw new RuntimeException(format("Unrecognized writeType '%s'", writeType));
        }
    }

    @Override
    public void postRun(TestCaseResult caseResult) {
        assertEquals(loopSize - 1, ref.getValue());

        long durationNs = caseResult.getLongProperty("duration(ns)");
        double nsPerSet = (loopSize * 1.0d) / durationNs;
        caseResult.put("operation(ns)", nsPerSet);
    }

    @TransactionalObject
    private static class LongRef {
        private long value;

        @TransactionalMethod(readonly = true)
        public long getValue() {
            return value;
        }

        public void loopSetter(long loopSize) {
            for (long k = 0; k < loopSize; k++) {
                set(k);
            }
        }

        public void loopField(long loopSize) {
            for (long k = 0; k < loopSize; k++) {
                value = k;
            }
        }

        public void loopLocal(long loopSize) {
            long tmp = value;

            for (long k = 0; k < loopSize; k++) {
                tmp = k;
            }

            value = tmp;
        }

        private void set(long newValue) {
            this.value = newValue;
        }
    }
}
