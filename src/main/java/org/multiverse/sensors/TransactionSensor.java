package org.multiverse.sensors;

import org.multiverse.api.TransactionConfiguration;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.String.format;

/**
 * A sensor responsible for measuring performance characteristics of transaction execution.
 *
 * @author Peter Veentjer.
 */
public final class TransactionSensor {

    private final AtomicLong startedCount = new AtomicLong();
    private final AtomicLong attemptCount = new AtomicLong();
    private final AtomicInteger maxTries = new AtomicInteger();
    private final AtomicLong completedCount = new AtomicLong();
    private final AtomicLong failedCount = new AtomicLong();

    private final TransactionConfiguration configuration;

    /**
     * Creates a new TransactionSensor.
     *
     * @param configuration the TransactionConfiguration this TransactionSensor measures.
     */
    public TransactionSensor(TransactionConfiguration configuration) {
        if (configuration == null) {
            throw new NullPointerException();
        }
        this.configuration = configuration;
    }

    public void signalExecution(int attempts, boolean success) {
        startedCount.incrementAndGet();
        attemptCount.addAndGet(attempts);

        if (success) {
            completedCount.incrementAndGet();
        } else {
            failedCount.incrementAndGet();
        }

        while (true) {
            int max = maxTries.get();
            if (attempts > max) {
                if (maxTries.compareAndSet(max, attempts)) {
                    break;
                }
            } else {
                break;
            }
        }
    }

    public String toString() {
        double avgRetriesPerTransaction = attemptCount.get() / (1d * startedCount.get());

        return format("[%s] started=%s completed=%s failed=%s attempts=%s avg-retries=%.2f max-retries=%s",
                configuration.getFamilyName(),
                startedCount.get(),
                completedCount.get(),
                failedCount.get(),
                attemptCount.get(),
                avgRetriesPerTransaction,
                maxTries);
    }
}
