package org.multiverse.stms.beta.sensors;

import org.multiverse.sensors.FrequencyDistribution;
import org.multiverse.sensors.Sensor;
import org.multiverse.sensors.StripedCounter;

/**
 * A sensor that can be used to track performance related information for transactions, so the number of
 * commits, aborts, retries etc.
 *
 * @author Peter Veentjer.
 */
public final class TransactionSensor implements Sensor {

    private final StripedCounter commitCounter;
    private final StripedCounter abortCounter;
    private final StripedCounter startedCounter;
    private final FrequencyDistribution frequencyDistribution;

    public TransactionSensor(int stripeSize){
        this.commitCounter = new StripedCounter(stripeSize);
        this.abortCounter = new StripedCounter(stripeSize);
        this.startedCounter = new StripedCounter(stripeSize);
        this.frequencyDistribution = new FrequencyDistribution();
    }

    @Override
    public void reset() {
        commitCounter.reset();
        abortCounter.reset();
        startedCounter.reset();
        frequencyDistribution.reset();
    }

    public void publish(int randomFactor, LocalTransactionStatistics statistics){
        commitCounter.inc(randomFactor, statistics.commitCount);
        abortCounter.inc(randomFactor,statistics.abortCount);
        startedCounter.inc(randomFactor, statistics.startedCount);

        statistics.abortCount = 0;
        statistics.commitCount = 0;
        statistics.startedCount = 0;
    }

    //public void 
}
