package org.multiverse.sensors;

import java.util.concurrent.atomic.AtomicLong;

/**
 * A striped counter that can be used as a scalable counter. The stripe is fixed, so doesn't grow if contended,
 * so make sure that the stripe is big enough.
 *
 * @author Peter Veentjer.
 */
public final class StripedCounter {

    private final AtomicLong[] stripe;

    /**
     * Creates a new StripedCounter.
     *
     * @param stripeLength the length of the stripe.
     * @throws IllegalArgumentException if stripeLength smaller or equal than 0.
     */
    public StripedCounter(int stripeLength) {
        if (stripeLength <= 0) {
            throw new IllegalArgumentException();
        }

        this.stripe = new AtomicLong[stripeLength];
        for (int k = 0; k < stripe.length; k++) {
            stripe[k] = new AtomicLong();
        }
    }

    /**
     * Returns the current count. It doesn't provide any form of read consistency, in most cases for a
     * performance counter this is not needed.
     *
     * @return the current count.
     */
    public long sum() {
        long sum = 0;
        for (int k = 0; k < stripe.length; k++) {
            sum += stripe[k].get();
        }
        return sum;
    }

    /**
     * Resets the counter. No consistency is provided.
     */
    public void reset(){
        for(int k=0;k<stripe.length;k++){
            stripe[k].set(0);
        }
    }

    /**
     * Increments the counter.
     *
     * If counter is 0, the call is ignored.
     *
     * @param randomFactor the random factor is needed to select a element of the stripe.
     * @param count the number to increase the counter with.
     */
    public void inc(final int randomFactor, final long count) {
        if (count == 0) {
            return;
        }

        int index = randomFactor % stripe.length;
        stripe[index].addAndGet(count);
    }
}
