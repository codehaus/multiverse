package org.multiverse.stms.beta.transactions;


/**
 * Transaction configuration that contains the speculative settings for the BetaTransactionConfig.
 * This object is immutable.
 */
public final class SpeculativeBetaConfig {

    private final boolean listenersRequired;
    private final int minimalLength;

    public SpeculativeBetaConfig() {
        this(false, 1);
    }

    public SpeculativeBetaConfig(boolean listenersRequired, int minimalLength) {
        if (minimalLength < 0) {
            throw new IllegalArgumentException();
        }
        this.listenersRequired = listenersRequired;
        this.minimalLength = minimalLength;
    }

    public boolean isListenerRequired() {
        return listenersRequired;
    }

    public int getMinimalLength() {
        return minimalLength;
    }

    public SpeculativeBetaConfig createWithMinimalLength(int newMinimalLength) {
        if (newMinimalLength < 0) {
            throw new IllegalArgumentException();
        }

        if (minimalLength >= newMinimalLength) {
            return this;
        }

        return new SpeculativeBetaConfig(listenersRequired, newMinimalLength);
    }

    public SpeculativeBetaConfig createWithListenersEnabled() {
        if (listenersRequired) {
            return this;
        }

        return new SpeculativeBetaConfig(true, minimalLength);
    }

    @Override
    public String toString() {
        return "SpeculativeBetaConfig{" +
                "listenersRequired=" + listenersRequired +
                ", minimalLength=" + minimalLength +
                '}';
    }
}
